package com.party.build.review.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.party.build.audit.domain.Message;
import com.party.build.audit.service.AuditService;
import com.party.build.audit.service.MessageService;
import com.party.build.auth.domain.SysUser;
import com.party.build.auth.mapper.SysUserMapper;
import com.party.build.auth.security.UserPrincipal;
import com.party.build.checklist.domain.MaterialType;
import com.party.build.checklist.mapper.MaterialTypeMapper;
import com.party.build.common.api.ErrorCode;
import com.party.build.common.api.PageResult;
import com.party.build.common.exception.BizException;
import com.party.build.person.domain.Person;
import com.party.build.person.mapper.PersonMapper;
import com.party.build.person.service.PersonService;
import com.party.build.review.domain.ReviewLog;
import com.party.build.review.dto.PendingReviewVO;
import com.party.build.review.dto.ReviewLogVO;
import com.party.build.review.dto.ReviewRequest;
import com.party.build.review.mapper.ReviewLogMapper;
import com.party.build.verify.domain.Material;
import com.party.build.verify.domain.MaterialVersion;
import com.party.build.verify.domain.VerifyReport;
import com.party.build.verify.domain.VerifyTask;
import com.party.build.verify.mapper.MaterialMapper;
import com.party.build.verify.mapper.MaterialVersionMapper;
import com.party.build.verify.mapper.VerifyReportMapper;
import com.party.build.verify.mapper.VerifyTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private static final List<String> TEMPLATES = List.of(
            "缺项：请补充材料中缺失的要素后重新上传。",
            "日期不符：请核对材料日期与阶段起始日期的时间逻辑。",
            "签名缺失：请补充本人签名/申请人署名。",
            "内容空泛：请结合个人经历补充具体事例，避免口号式表述。",
            "信息不一致：请核对家庭成员信息与登记表、政审材料是否一致。",
            "结论不明确：请补充明确的结论性意见。",
            "附件不清晰：请上传清晰、完整的扫描件。",
            "份数不足：请按清单要求补齐份数。",
            "格式不符：请按 PDF/JPG/PNG/DOCX 要求重新上传。",
            "其他：请根据核验报告中的具体发现项逐条整改。"
    );

    private final ReviewLogMapper reviewLogMapper;
    private final MaterialMapper materialMapper;
    private final MaterialVersionMapper versionMapper;
    private final VerifyReportMapper reportMapper;
    private final VerifyTaskMapper taskMapper;
    private final MaterialTypeMapper materialTypeMapper;
    private final PersonMapper personMapper;
    private final SysUserMapper userMapper;
    private final PersonService personService;
    private final MessageService messageService;
    private final AuditService auditService;

    public List<String> templates() {
        return TEMPLATES;
    }

    public PageResult<PendingReviewVO> pending(long page, long size, String stage, Long branchId,
                                               UserPrincipal principal) {
        LambdaQueryWrapper<Material> q = new LambdaQueryWrapper<Material>()
                .eq(Material::getStatus, "M3")
                .orderByDesc(Material::getMaterialId);
        if ("R2".equals(principal.getRoleCode()) || "R3".equals(principal.getRoleCode())) {
            List<Long> personIds = personMapper.selectList(new LambdaQueryWrapper<Person>()
                            .eq(Person::getBranchId, principal.getBranchId()))
                    .stream().map(Person::getPersonId).toList();
            if (personIds.isEmpty()) {
                return new PageResult<>(page, size, 0, List.of());
            }
            q.in(Material::getPersonId, personIds);
        } else if ("R1".equals(principal.getRoleCode())) {
            if (branchId != null) {
                List<Long> personIds = personMapper.selectList(new LambdaQueryWrapper<Person>()
                                .eq(Person::getBranchId, branchId))
                        .stream().map(Person::getPersonId).toList();
                if (personIds.isEmpty()) {
                    return new PageResult<>(page, size, 0, List.of());
                }
                q.in(Material::getPersonId, personIds);
            }
        } else {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        if (stage != null && !stage.isBlank()) {
            q.eq(Material::getStage, stage);
        }
        Page<Material> p = new Page<>(page, size);
        Page<Material> result = materialMapper.selectPage(p, q);
        List<PendingReviewVO> items = result.getRecords().stream().map(this::toPending).toList();
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), items);
    }

    @Transactional
    public ReviewLog review(Long materialId, ReviewRequest request, UserPrincipal principal) {
        Material material = materialMapper.selectById(materialId);
        if (material == null) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "材料不存在");
        }
        Person person = personService.getEntity(material.getPersonId(), principal);
        if (!"M3".equals(material.getStatus())) {
            throw new BizException(ErrorCode.STATE_NOT_ALLOWED, "只有待人工复核的材料可以复核");
        }
        if (material.getCurrentVersionId() == null || !material.getCurrentVersionId().equals(request.getVersionId())) {
            throw new BizException(ErrorCode.STATE_CHANGED, "材料版本已变化，请刷新");
        }
        MaterialVersion version = versionMapper.selectById(request.getVersionId());
        if (version == null || !version.getMaterialId().equals(materialId)) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "材料版本不存在");
        }
        VerifyReport report = resolveReport(materialId, version.getVersionId(), request.getReportId());
        String conclusion = request.getConclusion() == null ? "" : request.getConclusion().toUpperCase();
        if (!Set.of("PASS", "RETURN").contains(conclusion)) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "复核结论只能为 PASS 或 RETURN");
        }
        String level = "R3".equals(principal.getRoleCode()) ? "INITIAL" : "FINAL";
        if ("RETURN".equals(conclusion) && (request.getOpinion() == null || request.getOpinion().trim().length() < 10)) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "退回意见至少 10 个字，并具体到材料段落");
        }
        if ("RETURN".equals(conclusion) && (request.getReasonCode() == null || request.getReasonCode().isBlank())) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "退回必须选择原因");
        }

        ReviewLog log = new ReviewLog();
        log.setMaterialId(materialId);
        log.setVersionId(version.getVersionId());
        log.setReportId(report == null ? null : report.getReportId());
        log.setReviewerId(principal.getUserId());
        log.setReviewLevel(level);
        log.setConclusion(conclusion);
        log.setReasonCode(request.getReasonCode());
        log.setOpinion(request.getOpinion());
        reviewLogMapper.insert(log);

        material.setReviewerId(principal.getUserId());
        material.setLastReviewAt(LocalDateTime.now());
        if ("RETURN".equals(conclusion)) {
            material.setStatus("M5");
            materialMapper.updateById(material);
            messageService.sendReviewResult(material, version, "RETURN", request.getOpinion());
        } else if ("FINAL".equals(level)) {
            material.setStatus("M4");
            materialMapper.updateById(material);
            messageService.sendReviewResult(material, version, "PASS", null);
        } else {
            // 初审通过，仍停留在 M3，等待支部书记终审
            materialMapper.updateById(material);
        }
        auditService.log(principal, "REVIEW_MATERIAL", "MATERIAL", materialId,
                "SUCCESS", request.getOpinion(),
                "level=" + level + ", conclusion=" + conclusion);
        return log;
    }

    public List<ReviewLogVO> logs(Long materialId, UserPrincipal principal) {
        Material material = materialMapper.selectById(materialId);
        if (material == null) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "材料不存在");
        }
        personService.getEntity(material.getPersonId(), principal);
        List<ReviewLog> logs = reviewLogMapper.selectList(new LambdaQueryWrapper<ReviewLog>()
                .eq(ReviewLog::getMaterialId, materialId)
                .orderByDesc(ReviewLog::getReviewId));
        return logs.stream().map(log -> {
            ReviewLogVO vo = new ReviewLogVO();
            org.springframework.beans.BeanUtils.copyProperties(log, vo);
            SysUser reviewer = userMapper.selectById(log.getReviewerId());
            vo.setReviewerName(reviewer == null ? null : reviewer.getRealName());
            return vo;
        }).toList();
    }

    private VerifyReport resolveReport(Long materialId, Long versionId, Long reportId) {
        if (reportId != null) {
            VerifyReport report = reportMapper.selectById(reportId);
            if (report == null || !report.getMaterialId().equals(materialId) || !report.getVersionId().equals(versionId)) {
                throw new BizException(ErrorCode.VALIDATION_ERROR, "核验报告与材料版本不匹配");
            }
            return report;
        }
        return reportMapper.selectOne(new LambdaQueryWrapper<VerifyReport>()
                .eq(VerifyReport::getMaterialId, materialId)
                .eq(VerifyReport::getVersionId, versionId)
                .orderByDesc(VerifyReport::getReportId).last("limit 1"));
    }

    private PendingReviewVO toPending(Material material) {
        PendingReviewVO vo = new PendingReviewVO();
        vo.setMaterialId(material.getMaterialId());
        vo.setPersonId(material.getPersonId());
        Person person = personMapper.selectById(material.getPersonId());
        if (person != null) {
            vo.setPersonName(person.getName());
            vo.setStudentNo(person.getStudentNo());
        }
        vo.setStage(material.getStage());
        vo.setMaterialTypeId(material.getMaterialTypeId());
        MaterialType type = materialTypeMapper.selectById(material.getMaterialTypeId());
        vo.setMaterialTypeName(type == null ? null : type.getTypeName());
        vo.setMaterialStatus(material.getStatus());
        vo.setVersionId(material.getCurrentVersionId());
        if (material.getCurrentVersionId() != null) {
            MaterialVersion version = versionMapper.selectById(material.getCurrentVersionId());
            if (version != null) {
                vo.setVersionNo(version.getVersionNo());
                vo.setFileName(version.getFileName());
                vo.setLatestUploadedAt(version.getUploadedAt());
            }
        }
        VerifyReport report = reportMapper.selectOne(new LambdaQueryWrapper<VerifyReport>()
                .eq(VerifyReport::getMaterialId, material.getMaterialId())
                .orderByDesc(VerifyReport::getReportId).last("limit 1"));
        if (report != null) {
            vo.setReportId(report.getReportId());
            vo.setOverallConclusion(report.getOverallConclusion());
            vo.setRuleConclusion(report.getRuleConclusion());
            vo.setSemanticConclusion(report.getSemanticConclusion());
            vo.setReportDurationMs(report.getDurationMs());
            vo.setReportTime(report.getCreatedAt());
        }
        VerifyTask task = taskMapper.selectOne(new LambdaQueryWrapper<VerifyTask>()
                .eq(VerifyTask::getMaterialId, material.getMaterialId())
                .orderByDesc(VerifyTask::getTaskId).last("limit 1"));
        vo.setTaskStatus(task == null ? null : task.getStatus());
        Long initial = reviewLogMapper.selectCount(new LambdaQueryWrapper<ReviewLog>()
                .eq(ReviewLog::getMaterialId, material.getMaterialId())
                .eq(ReviewLog::getVersionId, material.getCurrentVersionId())
                .eq(ReviewLog::getReviewLevel, "INITIAL")
                .eq(ReviewLog::getConclusion, "PASS"));
        vo.setInitialReviewed(initial != null && initial > 0);
        return vo;
    }
}