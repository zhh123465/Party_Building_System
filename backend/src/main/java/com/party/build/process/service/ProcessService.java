package com.party.build.process.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.party.build.audit.service.AuditService;
import com.party.build.auth.security.UserPrincipal;
import com.party.build.checklist.domain.ChecklistItem;
import com.party.build.checklist.domain.ChecklistTemplate;
import com.party.build.checklist.domain.MaterialType;
import com.party.build.checklist.mapper.ChecklistItemMapper;
import com.party.build.checklist.mapper.MaterialTypeMapper;
import com.party.build.checklist.service.ChecklistService;
import com.party.build.common.api.ErrorCode;
import com.party.build.common.api.PageResult;
import com.party.build.common.enums.Stage;
import com.party.build.common.exception.BizException;
import com.party.build.person.domain.Person;
import com.party.build.person.mapper.PersonMapper;
import com.party.build.person.service.PersonService;
import com.party.build.process.domain.StageRecord;
import com.party.build.process.dto.*;
import com.party.build.process.guard.*;
import com.party.build.process.mapper.StageRecordMapper;
import com.party.build.verify.domain.Material;
import com.party.build.verify.domain.MaterialVersion;
import com.party.build.verify.mapper.MaterialMapper;
import com.party.build.verify.mapper.MaterialVersionMapper;
import com.party.build.verify.service.MaterialService;
import com.party.build.warning.domain.Warning;
import com.party.build.warning.mapper.WarningMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProcessService {
    private final PersonMapper personMapper;
    private final PersonService personService;
    private final ChecklistService checklistService;
    private final ChecklistItemMapper checklistItemMapper;
    private final MaterialTypeMapper materialTypeMapper;
    private final MaterialMapper materialMapper;
    private final MaterialVersionMapper versionMapper;
    private final WarningMapper warningMapper;
    private final StageRecordMapper stageRecordMapper;
    private final MaterialService materialService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final List<StageGuard> guards;

    public PageResult<BoardPersonVO> board(long page, long size, Long branchId, String stage, String keyword,
                                           UserPrincipal principal) {
        Page<Person> p = new Page<>(page, size);
        LambdaQueryWrapper<Person> q = scope(principal, branchId);
        if (stage != null && !stage.isBlank()) {
            q.eq(Person::getCurrentStage, stage);
        }
        if (keyword != null && !keyword.isBlank()) {
            q.and(w -> w.like(Person::getName, keyword).or().like(Person::getStudentNo, keyword));
        }
        q.orderByAsc(Person::getPersonId);
        Page<Person> result = personMapper.selectPage(p, q);
        List<BoardPersonVO> items = result.getRecords().stream().map(this::buildBoardPerson).toList();
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), items);
    }

    public Map<String, Object> overview(Long branchId, UserPrincipal principal) {
        List<Person> persons = personMapper.selectList(scope(principal, branchId));
        int locked = 0, red = 0, yellow = 0, green = 0, overdue = 0;
        for (Person person : persons) {
            BoardPersonVO vo = buildBoardPerson(person);
            if (vo.isLocked()) locked++;
            overdue += vo.getOverdueCount();
            switch (vo.getRiskLevel()) {
                case "RED" -> red++;
                case "YELLOW" -> yellow++;
                default -> green++;
            }
        }
        return Map.of("total", persons.size(), "locked", locked, "red", red,
                "yellow", yellow, "green", green, "overdue", overdue);
    }

    public List<StageDistributionVO> stageDistribution(Long branchId, UserPrincipal principal) {
        List<Person> persons = personMapper.selectList(scope(principal, branchId));
        Map<String, Long> counts = persons.stream()
                .collect(Collectors.groupingBy(Person::getCurrentStage, Collectors.counting()));
        return List.of("S0", "S1", "S2", "S3", "S4").stream()
                .map(s -> new StageDistributionVO(s, counts.getOrDefault(s, 0L)))
                .toList();
    }

    public AdvanceCheckVO advanceCheck(Long personId, UserPrincipal principal) {
        Person person = personService.getEntity(personId, principal);
        Stage next = Stage.of(person.getCurrentStage()).next();
        AdvanceCheckVO vo = new AdvanceCheckVO();
        vo.setPersonId(person.getPersonId());
        vo.setPersonName(person.getName());
        vo.setCurrentStage(person.getCurrentStage());
        if (next == null) {
            vo.setTargetStage(null);
            vo.setPassed(false);
            vo.setBlockers(List.of(new Blocker("STAGE", "当前阶段没有下一阶段", "查看材料", "S5 或未知阶段")));
            vo.setWarnings(List.of());
            vo.setRequiredFields(List.of());
            vo.setMaterialSummary(checkMaterials(person));
            return vo;
        }
        if (next == Stage.S5) {
            vo.setTargetStage("S5");
            vo.setPassed(false);
            vo.setBlockers(List.of(new Blocker("ARCHIVE", "S5 归档功能待 M9 实现", "联系管理员", "归档未实现")));
            vo.setWarnings(List.of());
            vo.setRequiredFields(List.of());
            vo.setMaterialSummary(checkMaterials(person));
            return vo;
        }
        return check(person, next, null, principal);
    }

    @Transactional
    public AdvanceCheckVO advance(Long personId, AdvanceRequest request, UserPrincipal principal) {
        Person person = personService.getEntity(personId, principal);
        Stage next = Stage.of(person.getCurrentStage()).next();
        if (next == null || next == Stage.S5) {
            throw new BizException(ErrorCode.STATE_NOT_ALLOWED, "当前阶段不能继续推进");
        }
        if (!next.name().equals(request.getTargetStage())) {
            throw new BizException(ErrorCode.STATE_CHANGED, "目标阶段与当前阶段不匹配，请刷新");
        }
        AdvanceCheckVO check = check(person, next, request, principal);
        if (!check.isPassed()) {
            String msg = check.getBlockers().stream().map(Blocker::getMessage).collect(Collectors.joining("；"));
            throw new BizException(ErrorCode.STAGE_LOCKED, msg);
        }
        String from = person.getCurrentStage();
        person.setCurrentStage(next.name());
        person.setStageStartDate(request.getMeetingDate());
        personMapper.updateById(person);

        StageRecord record = new StageRecord();
        record.setPersonId(person.getPersonId());
        record.setFromStage(from);
        record.setToStage(next.name());
        record.setChangeType("ADVANCE");
        record.setChangeDate(LocalDateTime.now());
        record.setOperatorId(principal.getUserId());
        record.setReason(request.getReason());
        record.setEvidence(writeEvidence(request));
        record.setTemplateId(person.getTemplateId());
        stageRecordMapper.insert(record);

        materialService.ensureMaterials(person);
        auditService.log(principal, "ADVANCE_STAGE", "PERSON", person.getPersonId(), "SUCCESS",
                request.getReason(), "from=" + from + " to=" + next.name());
        return check;
    }

    @Transactional
    public AdvanceCheckVO rollback(Long personId, RollbackRequest request, UserPrincipal principal) {
        if (!"R1".equals(principal.getRoleCode()) && !"R2".equals(principal.getRoleCode())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        Person person = personService.getEntity(personId, principal);
        Stage previous = Stage.of(person.getCurrentStage()).previous();
        if (previous == null) {
            throw new BizException(ErrorCode.STATE_NOT_ALLOWED, "当前阶段不能回退");
        }
        if (!previous.name().equals(request.getTargetStage())) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "只能回退到上一阶段");
        }
        if (request.getReason() == null || request.getReason().trim().length() < 10) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "回退理由至少 10 个字");
        }
        if (!request.isConfirm()) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "请勾选二次确认");
        }
        String from = person.getCurrentStage();
        person.setCurrentStage(previous.name());
        person.setStageStartDate(LocalDate.now());
        personMapper.updateById(person);

        StageRecord record = new StageRecord();
        record.setPersonId(person.getPersonId());
        record.setFromStage(from);
        record.setToStage(previous.name());
        record.setChangeType("ROLLBACK");
        record.setChangeDate(LocalDateTime.now());
        record.setOperatorId(principal.getUserId());
        record.setReason(request.getReason());
        record.setEvidence("{}");
        record.setTemplateId(person.getTemplateId());
        stageRecordMapper.insert(record);

        for (Material material : materialMapper.selectList(new LambdaQueryWrapper<Material>()
                .eq(Material::getPersonId, personId)
                .eq(Material::getStage, from))) {
            material.setStatus("M5");
            material.setReviewerId(null);
            material.setLastReviewAt(null);
            materialMapper.updateById(material);
        }
        auditService.log(principal, "ROLLBACK_STAGE", "PERSON", person.getPersonId(), "SUCCESS",
                request.getReason(), "from=" + from + " to=" + previous.name());

        AdvanceCheckVO vo = new AdvanceCheckVO();
        vo.setPersonId(person.getPersonId());
        vo.setPersonName(person.getName());
        vo.setCurrentStage(previous.name());
        vo.setTargetStage(previous.name());
        vo.setPassed(true);
        vo.setBlockers(List.of());
        vo.setWarnings(List.of());
        vo.setRequiredFields(List.of());
        vo.setMaterialSummary(checkMaterials(person));
        return vo;
    }

    private AdvanceCheckVO check(Person person, Stage target, AdvanceRequest request, UserPrincipal principal) {
        MaterialSummary summary = checkMaterials(person);
        List<Warning> warnings = warningMapper.selectList(new LambdaQueryWrapper<Warning>()
                .eq(Warning::getPersonId, person.getPersonId())
                .eq(Warning::getStage, person.getCurrentStage()));
        GuardContext context = new GuardContext(person, target.name(), request, summary, warnings, principal);
        List<Blocker> blockers = new ArrayList<>();
        List<String> warningList = new ArrayList<>();
        List<String> requiredFields = new ArrayList<>();
        for (StageGuard guard : guards) {
            GuardResult result = guard.check(context);
            blockers.addAll(result.getBlockers());
            warningList.addAll(result.getWarnings());
            requiredFields.addAll(result.getRequiredFields());
        }
        AdvanceCheckVO vo = new AdvanceCheckVO();
        vo.setPersonId(person.getPersonId());
        vo.setPersonName(person.getName());
        vo.setCurrentStage(person.getCurrentStage());
        vo.setTargetStage(target.name());
        vo.setPassed(blockers.isEmpty());
        vo.setBlockers(blockers);
        vo.setWarnings(warningList);
        vo.setRequiredFields(requiredFields.stream().distinct().toList());
        vo.setMaterialSummary(summary);
        return vo;
    }

    public MaterialSummary checkMaterials(Person person) {
        MaterialSummary summary = new MaterialSummary();
        List<String> issues = new ArrayList<>();
        ChecklistTemplate template = checklistService.getPublishedTemplate();
        Long templateId = person.getTemplateId() != null ? person.getTemplateId()
                : (template == null ? null : template.getTemplateId());
        if (templateId == null) {
            summary.setIssues(issues);
            summary.setPreviousStageIssues(new ArrayList<>());
            return summary;
        }
        List<ChecklistItem> items = checklistItemMapper.selectList(new LambdaQueryWrapper<ChecklistItem>()
                .eq(ChecklistItem::getTemplateId, templateId)
                .eq(ChecklistItem::getStage, person.getCurrentStage())
                .eq(ChecklistItem::getRequired, 1));
        List<Material> materials = materialMapper.selectList(new LambdaQueryWrapper<Material>()
                .eq(Material::getPersonId, person.getPersonId())
                .eq(Material::getStage, person.getCurrentStage()));
        Map<Long, Material> materialMap = materials.stream()
                .collect(Collectors.toMap(Material::getMaterialTypeId, m -> m, (a, b) -> a));
        Map<Long, MaterialType> typeMap = materialTypeMapper.selectList(null).stream()
                .collect(Collectors.toMap(MaterialType::getTypeId, t -> t));

        int required = items.size();
        int passed = 0;
        for (ChecklistItem item : items) {
            String name = typeMap.containsKey(item.getMaterialTypeId())
                    ? typeMap.get(item.getMaterialTypeId()).getTypeName() : ("材料#" + item.getMaterialTypeId());
            Material material = materialMap.get(item.getMaterialTypeId());
            if (material == null) {
                issues.add("缺少材料：" + name);
                summary.setMissingCount(summary.getMissingCount() + 1);
                continue;
            }
            if ("M5".equals(material.getStatus())) {
                summary.setReturnedCount(summary.getReturnedCount() + 1);
                issues.add("材料已退回：" + name);
                continue;
            }
            if (!List.of("M4", "M6").contains(material.getStatus())) {
                issues.add("材料未通过：" + name + "（当前 " + material.getStatus() + "）");
                continue;
            }
            Long versionCount = versionMapper.selectCount(new LambdaQueryWrapper<MaterialVersion>()
                    .eq(MaterialVersion::getMaterialId, material.getMaterialId()));
            if (item.getMinCount() != null && versionCount != null && versionCount < item.getMinCount()) {
                issues.add("份数不足：" + name + "（需 " + item.getMinCount() + " 份，当前 " + versionCount + " 份）");
                continue;
            }
            MaterialVersion latest = versionMapper.selectOne(new LambdaQueryWrapper<MaterialVersion>()
                    .eq(MaterialVersion::getMaterialId, material.getMaterialId())
                    .orderByDesc(MaterialVersion::getVersionNo).last("limit 1"));
            if (item.getMinPages() != null && item.getMinPages() > 0
                    && (latest == null || latest.getPageCount() == null || latest.getPageCount() < item.getMinPages())) {
                issues.add("页数不足：" + name + "（需 " + item.getMinPages() + " 页）");
                continue;
            }
            passed++;
        }
        summary.setRequiredCount(required);
        summary.setPassedCount(passed);
        summary.setPercent(required == 0 ? 100 : (int) Math.round(passed * 100.0 / required));
        summary.setIssues(issues);

        List<String> previousIssues = new ArrayList<>();
        Stage previous = Stage.of(person.getCurrentStage()).previous();
        if (previous != null) {
            List<Material> previousMaterials = materialMapper.selectList(new LambdaQueryWrapper<Material>()
                    .eq(Material::getPersonId, person.getPersonId())
                    .eq(Material::getStage, previous.name()));
            for (Material material : previousMaterials) {
                if (!List.of("M4", "M6").contains(material.getStatus())) {
                    String name = typeMap.containsKey(material.getMaterialTypeId())
                            ? typeMap.get(material.getMaterialTypeId()).getTypeName() : ("材料#" + material.getMaterialTypeId());
                    previousIssues.add("上一阶段材料未通过：" + name + "（" + material.getStatus() + "）");
                }
            }
        }
        summary.setPreviousStageIssues(previousIssues);
        return summary;
    }

    private BoardPersonVO buildBoardPerson(Person person) {
        materialService.ensureMaterials(person);
        MaterialSummary summary = checkMaterials(person);
        List<Warning> warnings = warningMapper.selectList(new LambdaQueryWrapper<Warning>()
                .eq(Warning::getPersonId, person.getPersonId())
                .eq(Warning::getStage, person.getCurrentStage())
                .eq(Warning::getStatus, "OPEN"));
        int overdue = (int) warnings.stream().filter(w -> "OVERDUE".equals(w.getLevel())).count();
        List<String> blockers = new ArrayList<>(summary.getIssues() == null ? List.of() : summary.getIssues());
        blockers.addAll(summary.getPreviousStageIssues() == null ? List.of() : summary.getPreviousStageIssues());
        for (Warning w : warnings) {
            if ("OVERDUE".equals(w.getLevel())) {
                blockers.add("节点逾期：" + w.getNodeCode() + "（应完成日 " + w.getDueDate() + "）");
            }
        }
        BoardPersonVO vo = new BoardPersonVO();
        vo.setPersonId(person.getPersonId());
        vo.setName(person.getName());
        vo.setStudentNo(person.getStudentNo());
        vo.setBranchId(person.getBranchId());
        vo.setCurrentStage(person.getCurrentStage());
        vo.setStageStartDate(person.getStageStartDate());
        vo.setRequiredCount(summary.getRequiredCount());
        vo.setPassedCount(summary.getPassedCount());
        vo.setPercent(summary.getPercent());
        vo.setOverdueCount(overdue);
        vo.setReturnedCount(summary.getReturnedCount());
        vo.setBlockerCount(blockers.size());
        vo.setLocked(!blockers.isEmpty());
        vo.setBlockers(blockers.stream().limit(4).toList());
        vo.setRiskLevel(overdue > 0 ? "RED" : (blockers.isEmpty() ? "GREEN" : "YELLOW"));
        return vo;
    }

    private LambdaQueryWrapper<Person> scope(UserPrincipal principal, Long branchId) {
        LambdaQueryWrapper<Person> q = new LambdaQueryWrapper<>();
        switch (principal.getRoleCode()) {
            case "R1" -> {
                if (branchId != null) q.eq(Person::getBranchId, branchId);
            }
            case "R2", "R3" -> q.eq(Person::getBranchId, principal.getBranchId());
            default -> throw new BizException(ErrorCode.FORBIDDEN);
        }
        return q;
    }

    private String writeEvidence(AdvanceRequest request) {
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("meetingDate", request.getMeetingDate());
            map.put("filingDate", request.getFilingDate());
            map.put("approvalDate", request.getApprovalDate());
            map.put("reason", request.getReason());
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }
}