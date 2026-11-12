package com.party.build.person.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.party.build.auth.domain.Branch;
import com.party.build.auth.mapper.BranchMapper;
import com.party.build.auth.security.UserPrincipal;
import com.party.build.audit.service.AuditService;
import com.party.build.common.api.ErrorCode;
import com.party.build.common.exception.BizException;
import com.party.build.common.util.CryptoService;
import com.party.build.person.domain.Person;
import com.party.build.person.dto.ImportResult;
import com.party.build.person.dto.PersonImportRow;
import com.party.build.person.mapper.PersonMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ImportService {
    private static final Pattern NAME = Pattern.compile("^[\\u4e00-\\u9fa5A-Za-z·]{2,20}$");
    private static final Pattern STUDENT_NO = Pattern.compile("^[A-Za-z0-9]{4,32}$");
    private static final Pattern PHONE = Pattern.compile("^1\\d{10}$");
    private static final Pattern ID_CARD = Pattern.compile("^\\d{17}[\\dXx]$");
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyy年M月d日")
    );

    private final PersonMapper personMapper;
    private final BranchMapper branchMapper;
    private final CryptoService cryptoService;
    private final AuditService auditService;

    public ImportResult importExcel(InputStream inputStream, UserPrincipal principal) {
        List<RowData> rows = new ArrayList<>();
        EasyExcel.read(inputStream, PersonImportRow.class, new AnalysisEventListener<PersonImportRow>() {
            @Override
            public void invoke(PersonImportRow data, AnalysisContext context) {
                rows.add(new RowData(context.readRowHolder().getRowIndex() + 1, data));
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
            }
        }).sheet().doRead();

        Map<String, Branch> branchMap = new HashMap<>();
        for (Branch b : branchMapper.selectList(null)) {
            branchMap.put(b.getBranchName(), b);
            branchMap.put(b.getBranchCode(), b);
        }

        ImportResult result = new ImportResult();
        result.setTotal(rows.size());
        Set<String> seen = new HashSet<>();
        for (RowData row : rows) {
            try {
                validate(row, branchMap, seen, principal);
                upsert(row.data, branchMap, principal);
                result.setSuccess(result.getSuccess() + 1);
            } catch (BizException e) {
                result.setFailed(result.getFailed() + 1);
                result.getErrors().add(new ImportResult.RowError(row.rowNo, e.getMessage().split(":")[0],
                        e.getMessage()));
            } catch (Exception e) {
                result.setFailed(result.getFailed() + 1);
                result.getErrors().add(new ImportResult.RowError(row.rowNo, "unknown", e.getMessage()));
            }
        }
        auditService.log(principal, "IMPORT_PERSON", "IMPORT", null,
                result.getFailed() == 0 ? "SUCCESS" : "PARTIAL",
                "成功 " + result.getSuccess() + " 行，失败 " + result.getFailed() + " 行", null);
        return result;
    }

    private void validate(RowData row, Map<String, Branch> branchMap, Set<String> seen, UserPrincipal principal) {
        PersonImportRow d = row.data;
        if (d.getName() == null || !NAME.matcher(d.getName().trim()).matches()) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "姓名: 格式不正确");
        }
        if (d.getStudentNo() == null || !STUDENT_NO.matcher(d.getStudentNo().trim()).matches()) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "学号: 格式不正确");
        }
        if (!seen.add(d.getStudentNo().trim())) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "学号: 文件内重复");
        }
        if (d.getGender() != null && !d.getGender().isBlank()
                && !Set.of("男", "女", "M", "F").contains(d.getGender().trim())) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "性别: 只能为男/女");
        }
        Branch branch = d.getBranchName() == null ? null : branchMap.get(d.getBranchName().trim());
        if (branch == null) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "所属支部: 不存在");
        }
        if (!"R1".equals(principal.getRoleCode()) && !branch.getBranchId().equals(principal.getBranchId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "所属支部: 超出数据范围");
        }
        if (d.getPhone() != null && !d.getPhone().isBlank() && !PHONE.matcher(d.getPhone().trim()).matches()) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "联系电话: 格式不正确");
        }
        if (d.getIdCard() != null && !d.getIdCard().isBlank() && !ID_CARD.matcher(d.getIdCard().trim()).matches()) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "身份证号: 格式不正确");
        }
        LocalDate app = parseDate(d.getApplicationDate(), "递交申请书日期");
        LocalDate start = parseDate(d.getStageStartDate(), "阶段起始日期");
        if (start.isBefore(app)) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "阶段起始日期: 不得早于申请书日期");
        }
        String stage = d.getCurrentStage() == null ? "S0" : d.getCurrentStage().trim().toUpperCase();
        if (!Set.of("S0", "S1", "S2", "S3", "S4").contains(stage)) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "当前阶段: 不在允许范围");
        }
    }

    private void upsert(PersonImportRow d, Map<String, Branch> branchMap, UserPrincipal principal) {
        String studentNo = d.getStudentNo().trim();
        Person existing = personMapper.selectOne(new LambdaQueryWrapper<Person>()
                .eq(Person::getStudentNo, studentNo).last("limit 1"));
        Person p = existing == null ? new Person() : existing;
        Branch branch = branchMap.get(d.getBranchName().trim());
        p.setStudentNo(studentNo);
        p.setName(d.getName().trim());
        p.setGender(normalizeGender(d.getGender()));
        p.setCollege(d.getCollege());
        p.setMajor(d.getMajor());
        p.setClassName(d.getClassName());
        p.setBranchId(branch.getBranchId());
        if (d.getPhone() != null && !d.getPhone().isBlank()) {
            p.setPhoneCipher(cryptoService.encrypt(d.getPhone().trim()));
            p.setPhoneMask(cryptoService.maskPhone(d.getPhone().trim()));
        }
        if (d.getIdCard() != null && !d.getIdCard().isBlank()) {
            p.setIdCardCipher(cryptoService.encrypt(d.getIdCard().trim()));
            p.setIdCardMask(cryptoService.maskIdCard(d.getIdCard().trim()));
        }
        p.setApplicationDate(parseDate(d.getApplicationDate(), "递交申请书日期"));
        p.setCurrentStage(d.getCurrentStage() == null ? "S0" : d.getCurrentStage().trim().toUpperCase());
        p.setStageStartDate(parseDate(d.getStageStartDate(), "阶段起始日期"));
        p.setTemplateId(1L);
        p.setStatus("ACTIVE");
        p.setRemark(d.getRemark());
        if (existing == null) {
            personMapper.insert(p);
        } else {
            personMapper.updateById(p);
        }
    }

    private String normalizeGender(String g) {
        if (g == null) {
            return null;
        }
        return switch (g.trim()) {
            case "男", "M", "m" -> "M";
            case "女", "F", "f" -> "F";
            default -> null;
        };
    }

    private LocalDate parseDate(String value, String column) {
        if (value == null || value.isBlank()) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, column + ": 不能为空");
        }
        for (DateTimeFormatter f : DATE_FORMATS) {
            try {
                return LocalDate.parse(value.trim(), f);
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new BizException(ErrorCode.VALIDATION_ERROR, column + ": 日期格式应为 yyyy-MM-dd");
    }

    private record RowData(int rowNo, PersonImportRow data) {
    }
}