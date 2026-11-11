package com.party.build.verify.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.party.build.auth.security.UserPrincipal;
import com.party.build.checklist.domain.ChecklistItem;
import com.party.build.checklist.domain.MaterialType;
import com.party.build.checklist.mapper.ChecklistItemMapper;
import com.party.build.checklist.mapper.MaterialTypeMapper;
import com.party.build.checklist.service.ChecklistService;
import com.party.build.common.api.ErrorCode;
import com.party.build.common.exception.BizException;
import com.party.build.person.domain.Person;
import com.party.build.person.service.PersonService;
import com.party.build.verify.domain.*;
import com.party.build.verify.dto.*;
import com.party.build.verify.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaterialService {
    private final MaterialMapper materialMapper;
    private final MaterialVersionMapper versionMapper;
    private final VerifyTaskMapper taskMapper;
    private final VerifyReportMapper reportMapper;
    private final VerifyFindingMapper findingMapper;
    private final ChecklistItemMapper checklistItemMapper;
    private final MaterialTypeMapper materialTypeMapper;
    private final ChecklistService checklistService;
    private final PersonService personService;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    @Value("${app.llm.provider:tokenrhythm}")
    private String llmProvider;
    @Value("${app.llm.model}")
    private String llmModel;

    public List<MaterialVO> listByPerson(Long personId, UserPrincipal principal) {
        Person person = personService.getEntity(personId, principal);
        ensureMaterials(person);
        List<Material> materials = materialMapper.selectList(new LambdaQueryWrapper<Material>()
                .eq(Material::getPersonId, personId)
                .orderByAsc(Material::getStage).orderByAsc(Material::getMaterialTypeId));
        Map<Long, MaterialType> typeMap = materialTypeMapper.selectList(null).stream()
                .collect(Collectors.toMap(MaterialType::getTypeId, t -> t));
        return materials.stream().map(m -> toVO(m, typeMap.get(m.getMaterialTypeId()))).toList();
    }

    public void ensureMaterials(Person person) {
        Long templateId = person.getTemplateId();
        if (templateId == null) {
            var published = checklistService.getPublishedTemplate();
            templateId = published == null ? null : published.getTemplateId();
        }
        if (templateId == null) {
            return;
        }
        List<ChecklistItem> items = checklistItemMapper.selectList(new LambdaQueryWrapper<ChecklistItem>()
                .eq(ChecklistItem::getTemplateId, templateId)
                .eq(ChecklistItem::getStage, person.getCurrentStage()));
        for (ChecklistItem item : items) {
            Long count = materialMapper.selectCount(new LambdaQueryWrapper<Material>()
                    .eq(Material::getPersonId, person.getPersonId())
                    .eq(Material::getStage, person.getCurrentStage())
                    .eq(Material::getMaterialTypeId, item.getMaterialTypeId()));
            if (count == null || count == 0) {
                Material material = new Material();
                material.setPersonId(person.getPersonId());
                material.setStage(person.getCurrentStage());
                material.setMaterialTypeId(item.getMaterialTypeId());
                material.setChecklistItemId(item.getItemId());
                material.setRequired(item.getRequired());
                material.setStatus("M1");
                material.setArchived(0);
                materialMapper.insert(material);
            }
        }
    }

    public MaterialDetailVO get(Long materialId, UserPrincipal principal) {
        Material material = requireMaterial(materialId, principal);
        MaterialDetailVO detail = new MaterialDetailVO();
        MaterialType type = materialTypeMapper.selectById(material.getMaterialTypeId());
        detail.setMaterial(toVO(material, type));
        detail.setVersions(versions(materialId));
        detail.setLatestReport(latestReport(materialId, principal));
        return detail;
    }

    public List<MaterialVersionVO> versions(Long materialId) {
        return versionMapper.selectList(new LambdaQueryWrapper<MaterialVersion>()
                        .eq(MaterialVersion::getMaterialId, materialId)
                        .orderByDesc(MaterialVersion::getVersionNo))
                .stream().map(this::toVersionVO).toList();
    }

    @Transactional
    public UploadResult upload(Long materialId, MultipartFile file, UserPrincipal principal) {
        Material material = requireMaterial(materialId, principal);
        if (List.of("M3", "M4", "M6").contains(material.getStatus())) {
            throw new BizException(ErrorCode.STATE_NOT_ALLOWED, "当前材料状态不允许重新上传");
        }
        Person person = personService.getEntity(material.getPersonId(), principal);
        int versionNo = nextVersionNo(materialId);
        FileStorageService.StoredFile stored = fileStorageService.save(file, person.getPersonId(), materialId, versionNo);
        Long dup = versionMapper.selectCount(new LambdaQueryWrapper<MaterialVersion>()
                .eq(MaterialVersion::getMaterialId, materialId)
                .eq(MaterialVersion::getSha256, stored.sha256()));
        if (dup != null && dup > 0) {
            throw new BizException(ErrorCode.ALREADY_EXISTS, "该文件已上传过");
        }

        MaterialVersion version = new MaterialVersion();
        version.setMaterialId(materialId);
        version.setVersionNo(versionNo);
        version.setFileName(stored.originalName());
        version.setFilePath(stored.relativePath());
        version.setFileSize(stored.size());
        version.setMimeType(stored.mimeType());
        version.setSha256(stored.sha256());
        version.setTextStatus("PENDING");
        version.setOcrUsed(0);
        version.setUploadedBy(principal.getUserId());
        version.setUploadedAt(LocalDateTime.now());
        versionMapper.insert(version);

        material.setStatus("M2");
        material.setCurrentVersionId(version.getVersionId());
        materialMapper.updateById(material);

        VerifyTask task = createTask(material, version);
        UploadResult result = new UploadResult();
        result.setMaterialId(materialId);
        result.setVersionId(version.getVersionId());
        result.setVersionNo(versionNo);
        result.setTaskId(task.getTaskId());
        result.setMaterialStatus(material.getStatus());
        result.setUploadedAt(version.getUploadedAt());
        return result;
    }

    public VerifyTask createVerifyTask(Long materialId, UserPrincipal principal) {
        Material material = requireMaterial(materialId, principal);
        if (material.getCurrentVersionId() == null) {
            throw new BizException(ErrorCode.STATE_NOT_ALLOWED, "请先上传材料");
        }
        VerifyTask running = taskMapper.selectOne(new LambdaQueryWrapper<VerifyTask>()
                .eq(VerifyTask::getVersionId, material.getCurrentVersionId())
                .in(VerifyTask::getStatus, List.of("PENDING", "RUNNING"))
                .orderByDesc(VerifyTask::getTaskId).last("limit 1"));
        if (running != null) {
            return running;
        }
        MaterialVersion version = versionMapper.selectById(material.getCurrentVersionId());
        material.setStatus("M2");
        materialMapper.updateById(material);
        return createTask(material, version);
    }

    public VerifyTask getTask(Long taskId, UserPrincipal principal) {
        VerifyTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "核验任务不存在");
        }
        requireMaterial(task.getMaterialId(), principal);
        return task;
    }

    public VerifyReportVO latestReport(Long materialId, UserPrincipal principal) {
        requireMaterial(materialId, principal);
        VerifyReport report = reportMapper.selectOne(new LambdaQueryWrapper<VerifyReport>()
                .eq(VerifyReport::getMaterialId, materialId)
                .orderByDesc(VerifyReport::getReportId).last("limit 1"));
        return report == null ? null : toReportVO(report);
    }

    public VerifyReportVO getReport(Long reportId, UserPrincipal principal) {
        VerifyReport report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "核验报告不存在");
        }
        requireMaterial(report.getMaterialId(), principal);
        return toReportVO(report);
    }

    public Path resolveVersionFile(Long materialId, Long versionId, UserPrincipal principal) {
        requireMaterial(materialId, principal);
        MaterialVersion version = versionMapper.selectById(versionId);
        if (version == null || !version.getMaterialId().equals(materialId)) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "材料版本不存在");
        }
        return fileStorageService.resolve(version.getFilePath());
    }

    public Material requireMaterial(Long materialId, UserPrincipal principal) {
        Material material = materialMapper.selectById(materialId);
        if (material == null) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "材料不存在");
        }
        personService.getEntity(material.getPersonId(), principal);
        return material;
    }

    private VerifyTask createTask(Material material, MaterialVersion version) {
        VerifyTask task = new VerifyTask();
        task.setMaterialId(material.getMaterialId());
        task.setVersionId(version.getVersionId());
        task.setStatus("PENDING");
        task.setAttemptNo(0);
        task.setModelProvider(llmProvider);
        task.setModelName(llmModel);
        task.setPromptVersion("v1");
        task.setQueuedAt(LocalDateTime.now());
        taskMapper.insert(task);
        return task;
    }

    private int nextVersionNo(Long materialId) {
        MaterialVersion latest = versionMapper.selectOne(new LambdaQueryWrapper<MaterialVersion>()
                .eq(MaterialVersion::getMaterialId, materialId)
                .orderByDesc(MaterialVersion::getVersionNo).last("limit 1"));
        return latest == null ? 1 : latest.getVersionNo() + 1;
    }

    private MaterialVO toVO(Material material, MaterialType type) {
        MaterialVO vo = new MaterialVO();
        BeanUtils.copyProperties(material, vo);
        if (type != null) {
            vo.setMaterialTypeName(type.getTypeName());
            vo.setMaterialTypeCode(type.getTypeCode());
        }
        MaterialVersion latest = versionMapper.selectOne(new LambdaQueryWrapper<MaterialVersion>()
                .eq(MaterialVersion::getMaterialId, material.getMaterialId())
                .orderByDesc(MaterialVersion::getVersionNo).last("limit 1"));
        if (latest != null) {
            vo.setLatestVersionNo(latest.getVersionNo());
            vo.setLatestFileName(latest.getFileName());
            vo.setLatestUploadedAt(latest.getUploadedAt());
        }
        VerifyTask task = taskMapper.selectOne(new LambdaQueryWrapper<VerifyTask>()
                .eq(VerifyTask::getMaterialId, material.getMaterialId())
                .orderByDesc(VerifyTask::getTaskId).last("limit 1"));
        if (task != null) {
            vo.setTaskStatus(task.getStatus());
        }
        VerifyReport report = reportMapper.selectOne(new LambdaQueryWrapper<VerifyReport>()
                .eq(VerifyReport::getMaterialId, material.getMaterialId())
                .orderByDesc(VerifyReport::getReportId).last("limit 1"));
        if (report != null) {
            vo.setReportId(report.getReportId());
            vo.setOverallConclusion(report.getOverallConclusion());
        }
        if (material.getChecklistItemId() != null) {
            ChecklistItem item = checklistItemMapper.selectById(material.getChecklistItemId());
            if (item != null) {
                vo.setVerifyPoints(readPoints(item.getVerifyPoints()));
            }
        }
        return vo;
    }

    private MaterialVersionVO toVersionVO(MaterialVersion version) {
        MaterialVersionVO vo = new MaterialVersionVO();
        BeanUtils.copyProperties(version, vo);
        return vo;
    }

    private VerifyReportVO toReportVO(VerifyReport report) {
        VerifyReportVO vo = new VerifyReportVO();
        BeanUtils.copyProperties(report, vo);
        List<VerifyFinding> findings = findingMapper.selectList(new LambdaQueryWrapper<VerifyFinding>()
                .eq(VerifyFinding::getReportId, report.getReportId())
                .orderByAsc(VerifyFinding::getLayer).orderByAsc(VerifyFinding::getFindingId));
        vo.setFindings(findings.stream().map(f -> {
            VerifyReportVO.FindingVO fv = new VerifyReportVO.FindingVO();
            BeanUtils.copyProperties(f, fv);
            return fv;
        }).toList());
        return vo;
    }

    private List<String> readPoints(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }
}