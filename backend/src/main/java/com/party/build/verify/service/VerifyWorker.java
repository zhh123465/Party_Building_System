package com.party.build.verify.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.party.build.checklist.domain.ChecklistItem;
import com.party.build.checklist.mapper.ChecklistItemMapper;
import com.party.build.common.api.ErrorCode;
import com.party.build.common.exception.BizException;
import com.party.build.person.domain.Person;
import com.party.build.person.mapper.PersonMapper;
import com.party.build.verify.ai.SemanticVerifier;
import com.party.build.verify.domain.*;
import com.party.build.verify.mapper.*;
import com.party.build.verify.ocr.ExtractResult;
import com.party.build.verify.ocr.TextExtractService;
import com.party.build.verify.rule.Finding;
import com.party.build.verify.rule.RuleEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerifyWorker {
    private final VerifyTaskMapper taskMapper;
    private final MaterialMapper materialMapper;
    private final MaterialVersionMapper versionMapper;
    private final VerifyReportMapper reportMapper;
    private final VerifyFindingMapper findingMapper;
    private final VerifyRuleMapper ruleMapper;
    private final ChecklistItemMapper checklistItemMapper;
    private final PersonMapper personMapper;
    private final TextExtractService textExtractService;
    private final RuleEngine ruleEngine;
    private final SemanticVerifier semanticVerifier;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${app.verify.poll-ms:3000}")
    public void poll() {
        VerifyTask task = taskMapper.selectOne(new LambdaQueryWrapper<VerifyTask>()
                .eq(VerifyTask::getStatus, "PENDING")
                .orderByAsc(VerifyTask::getQueuedAt)
                .last("limit 1"));
        if (task == null) {
            return;
        }
        int claimed = taskMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<VerifyTask>()
                .eq(VerifyTask::getTaskId, task.getTaskId())
                .eq(VerifyTask::getStatus, "PENDING")
                .set(VerifyTask::getStatus, "RUNNING")
                .set(VerifyTask::getStartedAt, LocalDateTime.now())
                .set(VerifyTask::getAttemptNo, task.getAttemptNo() == null ? 1 : task.getAttemptNo() + 1));
        if (claimed == 0) {
            return;
        }
        process(task.getTaskId());
    }

    private void process(Long taskId) {
        long start = System.currentTimeMillis();
        VerifyTask task = taskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        try {
            Material material = materialMapper.selectById(task.getMaterialId());
            MaterialVersion version = versionMapper.selectById(task.getVersionId());
            if (material == null || version == null) {
                throw new BizException(ErrorCode.VALIDATION_ERROR, "任务关联的材料/版本不存在");
            }
            Person person = personMapper.selectById(material.getPersonId());
            if (person == null) {
                throw new BizException(ErrorCode.VALIDATION_ERROR, "任务关联的人员不存在");
            }

            ExtractResult extracted;
            try (InputStream in = Files.newInputStream(fileStorageService.resolve(version.getFilePath()))) {
                extracted = textExtractService.extract(in, version.getFileName());
            }
            version.setTextContent(extracted.getText());
            version.setTextStatus("OK");
            version.setPageCount(extracted.getPageCount());
            version.setOcrUsed(extracted.isOcrUsed() ? 1 : 0);
            versionMapper.updateById(version);

            List<VerifyRule> rules = ruleMapper.selectList(new LambdaQueryWrapper<VerifyRule>()
                    .eq(VerifyRule::getEnabled, 1)
                    .eq(VerifyRule::getStage, material.getStage())
                    .and(w -> w.eq(VerifyRule::getMaterialTypeId, material.getMaterialTypeId())
                            .or().isNull(VerifyRule::getMaterialTypeId)));
            List<Finding> ruleFindings = ruleEngine.evaluateWithRules(extracted.getText(), rules);

            List<String> points = points(material);
            List<Finding> semanticFindings;
            String semanticError = null;
            try {
                semanticFindings = semanticVerifier.verify(extracted.getText(), points);
            } catch (BizException e) {
                semanticError = e.getMessage();
                semanticFindings = List.of(new Finding("SEMANTIC", "", "语义核验", "UNKNOWN", "SOFT",
                        "", "模型不可用，已转人工处理", 0.0));
            }

            String overall = overall(ruleFindings, semanticFindings);
            VerifyReport report = new VerifyReport();
            report.setMaterialId(material.getMaterialId());
            report.setVersionId(version.getVersionId());
            report.setTaskId(taskId);
            report.setRuleConclusion(ruleConclusion(ruleFindings));
            report.setSemanticConclusion(semanticConclusion(semanticFindings));
            report.setOverallConclusion(overall);
            report.setConfidence(BigDecimal.valueOf(0.8));
            report.setRuleVersion("v1");
            report.setModelName(task.getModelName());
            report.setPromptVersion(task.getPromptVersion());
            report.setEvidence(writeEvidence(ruleFindings, semanticFindings));
            report.setDurationMs((int) (System.currentTimeMillis() - start));
            reportMapper.insert(report);
            saveFindings(report.getReportId(), ruleFindings, semanticFindings);

            material.setStatus(semanticError == null ? "M3" : "M7");
            materialMapper.updateById(material);

            task.setStatus(semanticError == null ? "SUCCESS" : "FAILED");
            task.setErrorCode(semanticError == null ? null : "40001");
            task.setErrorMsg(semanticError);
            task.setFinishedAt(LocalDateTime.now());
            task.setDurationMs((int) (System.currentTimeMillis() - start));
            taskMapper.updateById(task);
        } catch (Exception e) {
            log.warn("verify task {} failed", taskId, e);
            task.setStatus("FAILED");
            task.setErrorCode(e instanceof BizException be ? String.valueOf(be.getCode()) : "50003");
            task.setErrorMsg(truncate(e.getMessage(), 480));
            task.setFinishedAt(LocalDateTime.now());
            task.setDurationMs((int) (System.currentTimeMillis() - start));
            taskMapper.updateById(task);
            Material material = materialMapper.selectById(task.getMaterialId());
            if (material != null) {
                material.setStatus("M7");
                materialMapper.updateById(material);
            }
        }
    }

    private void saveFindings(Long reportId, List<Finding> rules, List<Finding> semantics) {
        List<Finding> all = new ArrayList<>(rules);
        all.addAll(semantics);
        for (Finding f : all) {
            VerifyFinding entity = new VerifyFinding();
            entity.setReportId(reportId);
            entity.setLayer(f.getLayer());
            entity.setRuleCode(f.getRuleCode());
            entity.setPointName(f.getPoint());
            entity.setConclusion(f.getConclusion());
            entity.setSeverity(f.getSeverity());
            entity.setEvidenceText(truncate(f.getEvidence(), 1900));
            entity.setSuggestion(truncate(f.getSuggestion(), 900));
            entity.setConfidence(f.getConfidence() == null ? null : BigDecimal.valueOf(f.getConfidence()));
            findingMapper.insert(entity);
        }
    }

    private List<String> points(Material material) {
        if (material.getChecklistItemId() != null) {
            ChecklistItem item = checklistItemMapper.selectById(material.getChecklistItemId());
            if (item != null && item.getVerifyPoints() != null && !item.getVerifyPoints().isBlank()) {
                try {
                    return objectMapper.readValue(item.getVerifyPoints(), new com.fasterxml.jackson.core.type.TypeReference<>() {
                    });
                } catch (Exception ignored) {
                }
            }
        }
        return List.of("材料内容是否完整", "是否存在空泛套话", "是否与本人情况一致");
    }

    private String ruleConclusion(List<Finding> findings) {
        if (findings.stream().anyMatch(f -> "ERROR".equals(f.getConclusion()))) {
            return "ERROR";
        }
        if (findings.stream().anyMatch(f -> "MISSING".equals(f.getConclusion()))) {
            return "MISSING";
        }
        return "PASS";
    }

    private String semanticConclusion(List<Finding> findings) {
        if (findings.stream().anyMatch(f -> "CONFLICT".equals(f.getConclusion()))) {
            return "CONFLICT";
        }
        if (findings.stream().anyMatch(f -> "DOUBT".equals(f.getConclusion()))) {
            return "DOUBT";
        }
        if (findings.stream().anyMatch(f -> "UNKNOWN".equals(f.getConclusion()))) {
            return "UNKNOWN";
        }
        return "PASS";
    }

    private String overall(List<Finding> rules, List<Finding> semantics) {
        boolean hardMissing = rules.stream()
                .anyMatch(f -> "HARD".equals(f.getSeverity()) && "MISSING".equals(f.getConclusion()));
        if (hardMissing || semantics.stream().anyMatch(f -> "CONFLICT".equals(f.getConclusion()))) {
            return "NEED_FIX";
        }
        if (semantics.stream().anyMatch(f -> "DOUBT".equals(f.getConclusion()) || "UNKNOWN".equals(f.getConclusion()))) {
            return "DOUBT";
        }
        return "PASS";
    }

    private String writeEvidence(List<Finding> rules, List<Finding> semantics) {
        try {
            return objectMapper.writeValueAsString(java.util.Map.of("rule", rules, "semantic", semantics));
        } catch (Exception e) {
            return "{}";
        }
    }

    private String truncate(String text, int max) {
        return text == null || text.length() <= max ? text : text.substring(0, max);
    }
}