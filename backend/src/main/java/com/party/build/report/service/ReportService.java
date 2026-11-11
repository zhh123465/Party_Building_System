package com.party.build.report.service;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.party.build.auth.domain.Branch;
import com.party.build.auth.mapper.BranchMapper;
import com.party.build.auth.security.UserPrincipal;
import com.party.build.common.api.ErrorCode;
import com.party.build.common.exception.BizException;
import com.party.build.person.domain.Person;
import com.party.build.person.mapper.PersonMapper;
import com.party.build.process.dto.MaterialSummary;
import com.party.build.process.service.ProcessService;
import com.party.build.report.domain.ExportLog;
import com.party.build.report.dto.*;
import com.party.build.report.mapper.ExportLogMapper;
import com.party.build.verify.domain.Material;
import com.party.build.verify.domain.VerifyFinding;
import com.party.build.verify.domain.VerifyReport;
import com.party.build.verify.mapper.MaterialMapper;
import com.party.build.verify.mapper.VerifyFindingMapper;
import com.party.build.verify.mapper.VerifyReportMapper;
import com.party.build.warning.domain.Warning;
import com.party.build.warning.mapper.WarningMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final PersonMapper personMapper;
    private final BranchMapper branchMapper;
    private final MaterialMapper materialMapper;
    private final VerifyReportMapper reportMapper;
    private final VerifyFindingMapper findingMapper;
    private final WarningMapper warningMapper;
    private final ProcessService processService;
    private final ExportLogMapper exportLogMapper;

    public List<StageCountVO> stageCount(Long branchId, UserPrincipal principal) {
        List<Person> persons = persons(principal, branchId);
        Map<String, Long> counts = persons.stream()
                .collect(Collectors.groupingBy(Person::getCurrentStage, Collectors.counting()));
        return List.of("S0", "S1", "S2", "S3", "S4").stream()
                .map(s -> new StageCountVO(s, counts.getOrDefault(s, 0L)))
                .toList();
    }

    public ReportOverviewVO overview(Long branchId, UserPrincipal principal) {
        List<Person> persons = persons(principal, branchId);
        List<ComplianceVO> compliance = complianceList(branchId, principal);
        int required = 0, passed = 0;
        long overdue = 0;
        double days = 0;
        int dayCount = 0;
        for (Person person : persons) {
            MaterialSummary summary = processService.checkMaterials(person);
            required += summary.getRequiredCount();
            passed += summary.getPassedCount();
            if (person.getStageStartDate() != null) {
                days += ChronoUnit.DAYS.between(person.getStageStartDate(), LocalDate.now());
                dayCount++;
            }
        }
        for (ComplianceVO c : compliance) {
            overdue += c.getOverdue();
        }
        ReportOverviewVO vo = new ReportOverviewVO();
        vo.setTotalPersons(persons.size());
        vo.setActivePersons(persons.stream().filter(p -> "ACTIVE".equals(p.getStatus())).count());
        vo.setTotalRequired(required);
        vo.setTotalPassed(passed);
        vo.setCompletionRate(required == 0 ? 100 : Math.round(passed * 10000.0 / required) / 100.0);
        vo.setOverdueWarnings(overdue);
        vo.setAvgDaysInStage(dayCount == 0 ? 0 : Math.round(days / dayCount * 10.0) / 10.0);
        vo.setAverageCompliance(compliance.stream().mapToDouble(ComplianceVO::getTotalScore).average().orElse(100));
        vo.setStageCounts(stageCount(branchId, principal));
        return vo;
    }

    public List<ComplianceVO> complianceList(Long branchId, UserPrincipal principal) {
        List<Person> persons = persons(principal, branchId);
        Map<Long, String> branchNames = branchMapper.selectList(null).stream()
                .collect(Collectors.toMap(Branch::getBranchId, Branch::getBranchName));
        return persons.stream().map(person -> {
            ComplianceVO vo = calculateCompliance(person);
            vo.setBranchName(branchNames.getOrDefault(person.getBranchId(), ""));
            return vo;
        }).sorted(Comparator.comparingDouble(ComplianceVO::getTotalScore)).toList();
    }

    public List<HeatmapCellVO> heatmap(Long branchId, UserPrincipal principal) {
        List<ComplianceVO> list = complianceList(branchId, principal);
        Map<String, List<ComplianceVO>> groups = list.stream()
                .collect(Collectors.groupingBy(c -> c.getBranchId() + "|" + c.getStage()));
        List<HeatmapCellVO> cells = new ArrayList<>();
        for (List<ComplianceVO> group : groups.values()) {
            ComplianceVO first = group.get(0);
            double avg = group.stream().mapToDouble(ComplianceVO::getTotalScore).average().orElse(0);
            HeatmapCellVO cell = new HeatmapCellVO();
            cell.setBranchId(first.getBranchId());
            cell.setBranchName(first.getBranchName());
            cell.setStage(first.getStage());
            cell.setAvgScore(Math.round(avg * 100.0) / 100.0);
            cell.setCount(group.size());
            cell.setRiskLevel(avg < 60 ? "RED" : avg < 80 ? "YELLOW" : "GREEN");
            cells.add(cell);
        }
        cells.sort(Comparator.comparing(HeatmapCellVO::getBranchId).thenComparing(HeatmapCellVO::getStage));
        return cells;
    }

    public void export(String type, Long branchId, UserPrincipal principal, HttpServletResponse response) {
        List<ComplianceVO> list = complianceList(branchId, principal);
        Map<Long, Person> personMap = personMapper.selectList(null).stream()
                .collect(Collectors.toMap(Person::getPersonId, p -> p));
        List<PersonExportRow> rows = list.stream().map(c -> {
            Person person = personMap.get(c.getPersonId());
            PersonExportRow row = new PersonExportRow();
            row.setName(c.getName());
            row.setStudentNo(c.getStudentNo());
            row.setBranchName(c.getBranchName());
            row.setStage(c.getStage());
            row.setStageStartDate(person == null || person.getStageStartDate() == null ? "" : person.getStageStartDate().toString());
            row.setRequired(c.getRequired());
            row.setPassed(c.getPassed());
            row.setCompletion(c.getRequired() == 0 ? "100%" : Math.round(c.getPassed() * 10000.0 / c.getRequired()) / 100.0 + "%");
            row.setCompliance(c.getTotalScore());
            row.setRiskLevel(c.getRiskLevel());
            row.setOverdue(c.getOverdue());
            return row;
        }).toList();
        if (rows.size() > 10000) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "单次导出不超过 10000 行，请缩小筛选范围");
        }
        try {
            String fileName = URLEncoder.encode("人员发展统计报表", StandardCharsets.UTF_8).replace("+", "%20");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + fileName + ".xlsx");
            EasyExcel.write(response.getOutputStream(), PersonExportRow.class)
                    .sheet("人员统计").doWrite(rows);

            ExportLog log = new ExportLog();
            log.setUserId(principal.getUserId());
            log.setExportType(type == null ? "PERSON" : type);
            log.setFileName("人员发展统计报表.xlsx");
            log.setFilePath("stream");
            log.setWatermarkText("姓名+用途+时间（POC 未叠加文件水印）");
            log.setRowCount(rows.size());
            exportLogMapper.insert(log);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ErrorCode.FILE_STORAGE_ERROR, "导出失败: " + e.getMessage());
        }
    }

    private ComplianceVO calculateCompliance(Person person) {
        MaterialSummary summary = processService.checkMaterials(person);
        int overdue = 0, urgent = 0, near = 0, info = 0;
        List<Warning> warnings = warningMapper.selectList(new LambdaQueryWrapper<Warning>()
                .eq(Warning::getPersonId, person.getPersonId())
                .eq(Warning::getStage, person.getCurrentStage())
                .eq(Warning::getStatus, "OPEN"));
        for (Warning w : warnings) {
            switch (w.getLevel()) {
                case "OVERDUE" -> overdue++;
                case "URGENT" -> urgent++;
                case "NEAR" -> near++;
                case "INFO" -> info++;
                default -> {
                }
            }
        }
        int conflict = 0, doubt = 0, lowConfidence = 0, semanticTotal = 0;
        List<Material> materials = materialMapper.selectList(new LambdaQueryWrapper<Material>()
                .eq(Material::getPersonId, person.getPersonId())
                .eq(Material::getStage, person.getCurrentStage()));
        for (Material material : materials) {
            VerifyReport report = reportMapper.selectOne(new LambdaQueryWrapper<VerifyReport>()
                    .eq(VerifyReport::getMaterialId, material.getMaterialId())
                    .eq(material.getCurrentVersionId() != null, VerifyReport::getVersionId, material.getCurrentVersionId())
                    .orderByDesc(VerifyReport::getReportId).last("limit 1"));
            if (report == null) {
                continue;
            }
            List<VerifyFinding> findings = findingMapper.selectList(new LambdaQueryWrapper<VerifyFinding>()
                    .eq(VerifyFinding::getReportId, report.getReportId())
                    .eq(VerifyFinding::getLayer, "SEMANTIC"));
            for (VerifyFinding f : findings) {
                semanticTotal++;
                if ("CONFLICT".equals(f.getConclusion())) conflict++;
                if ("DOUBT".equals(f.getConclusion())) doubt++;
                if (f.getConfidence() != null && f.getConfidence().doubleValue() < 0.6) lowConfidence++;
            }
        }
        ComplianceCalculator.Result r = ComplianceCalculator.calculate(
                summary.getRequiredCount(), summary.getPassedCount(),
                overdue, urgent, near, info,
                conflict, doubt, 0, lowConfidence, semanticTotal);
        ComplianceVO vo = new ComplianceVO();
        vo.setPersonId(person.getPersonId());
        vo.setName(person.getName());
        vo.setStudentNo(person.getStudentNo());
        vo.setBranchId(person.getBranchId());
        vo.setStage(person.getCurrentStage());
        vo.setRequired(summary.getRequiredCount());
        vo.setPassed(summary.getPassedCount());
        vo.setMaterialScore(r.materialScore());
        vo.setTimeScore(r.timeScore());
        vo.setContentScore(r.contentScore());
        vo.setTotalScore(r.totalScore());
        vo.setRiskLevel(r.riskLevel());
        vo.setOverdue(overdue);
        vo.setUrgent(urgent);
        vo.setNear(near);
        vo.setInfo(info);
        vo.setConflict(conflict);
        vo.setDoubt(doubt);
        vo.setLowConfidence(lowConfidence);
        vo.setSemanticTotal(semanticTotal);
        return vo;
    }

    private List<Person> persons(UserPrincipal principal, Long branchId) {
        LambdaQueryWrapper<Person> q = new LambdaQueryWrapper<>();
        switch (principal.getRoleCode()) {
            case "R1" -> {
                if (branchId != null) q.eq(Person::getBranchId, branchId);
            }
            case "R2", "R3" -> q.eq(Person::getBranchId, principal.getBranchId());
            default -> throw new BizException(ErrorCode.FORBIDDEN);
        }
        q.orderByAsc(Person::getPersonId);
        return personMapper.selectList(q);
    }
}