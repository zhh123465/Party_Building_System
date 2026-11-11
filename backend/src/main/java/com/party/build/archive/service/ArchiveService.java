package com.party.build.archive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.party.build.archive.domain.ArchiveRecord;
import com.party.build.archive.dto.*;
import com.party.build.archive.mapper.ArchiveRecordMapper;
import com.party.build.audit.service.AuditService;
import com.party.build.auth.domain.SysUser;
import com.party.build.auth.mapper.SysUserMapper;
import com.party.build.auth.security.UserPrincipal;
import com.party.build.checklist.domain.ChecklistItem;
import com.party.build.checklist.domain.ChecklistTemplate;
import com.party.build.checklist.domain.MaterialType;
import com.party.build.checklist.mapper.ChecklistItemMapper;
import com.party.build.checklist.mapper.MaterialTypeMapper;
import com.party.build.checklist.service.ChecklistService;
import com.party.build.common.api.ErrorCode;
import com.party.build.common.api.PageResult;
import com.party.build.common.exception.BizException;
import com.party.build.person.domain.Person;
import com.party.build.person.mapper.PersonMapper;
import com.party.build.person.service.PersonService;
import com.party.build.report.domain.ExportLog;
import com.party.build.report.mapper.ExportLogMapper;
import com.party.build.common.enums.Stage;
import com.party.build.verify.domain.Material;
import com.party.build.verify.domain.MaterialVersion;
import com.party.build.verify.mapper.MaterialMapper;
import com.party.build.verify.mapper.MaterialVersionMapper;
import com.party.build.verify.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class ArchiveService {
    private final PersonMapper personMapper;
    private final PersonService personService;
    private final ChecklistService checklistService;
    private final ChecklistItemMapper checklistItemMapper;
    private final MaterialTypeMapper materialTypeMapper;
    private final MaterialMapper materialMapper;
    private final MaterialVersionMapper versionMapper;
    private final ArchiveRecordMapper archiveRecordMapper;
    private final FileStorageService fileStorageService;
    private final SysUserMapper userMapper;
    private final ExportLogMapper exportLogMapper;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public PersonArchiveVO tree(Long personId, UserPrincipal principal) {
        Person person = personService.getEntity(personId, principal);
        Long templateId = templateId(person);
        List<ArchiveStageVO> stages = new ArrayList<>();
        Stage current = Stage.of(person.getCurrentStage());
        for (Stage stage : Stage.activeStages()) {
            if (current != null && stage.ordinal() <= current.ordinal()) {
                stages.add(toStageVO(person, stage.name(), templateId));
            }
        }
        PersonArchiveVO vo = new PersonArchiveVO();
        vo.setPersonId(person.getPersonId());
        vo.setName(person.getName());
        vo.setStudentNo(person.getStudentNo());
        vo.setCurrentStage(person.getCurrentStage());
        vo.setStages(stages);
        vo.setArchives(archives(personId, principal));
        return vo;
    }

    @Transactional
    public ArchiveRecordVO archive(Long personId, String stage, UserPrincipal principal) {
        Person person = personService.getEntity(personId, principal);
        Stage.of(stage);
        ArchiveRecord existing = archiveRecordMapper.selectOne(new LambdaQueryWrapper<ArchiveRecord>()
                .eq(ArchiveRecord::getPersonId, personId)
                .eq(ArchiveRecord::getStage, stage).last("limit 1"));
        if (existing != null && "READY".equals(existing.getStatus())) {
            return toRecordVO(existing);
        }
        Long templateId = templateId(person);
        List<String> issues = issuesForStage(person, stage, templateId);
        if (!issues.isEmpty()) {
            throw new BizException(ErrorCode.MATERIAL_INCOMPLETE, String.join("；", issues));
        }
        List<Material> materials = materials(personId, stage);
        Map<Long, MaterialType> typeMap = materialTypeMapper.selectList(null).stream()
                .collect(Collectors.toMap(MaterialType::getTypeId, t -> t));
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String archiveNo = "AR-" + personId + "-" + stage + "-" + timestamp;
            String relative = "archive/" + personId + "/" + stage + "-" + timestamp + ".zip";
            Path zip = fileStorageService.root().resolve(relative).normalize();
            Files.createDirectories(zip.getParent());
            String manifestJson = buildZip(zip, person, stage, materials, typeMap, principal);
            ArchiveRecord record = existing == null ? new ArchiveRecord() : existing;
            record.setPersonId(personId);
            record.setStage(stage);
            record.setArchiveNo(archiveNo);
            record.setManifest(manifestJson);
            record.setFilePath(relative);
            record.setStatus("READY");
            record.setArchivedBy(principal.getUserId());
            record.setArchivedAt(LocalDateTime.now());
            if (existing == null) {
                archiveRecordMapper.insert(record);
            } else {
                archiveRecordMapper.updateById(record);
            }
            for (Material material : materials) {
                material.setStatus("M6");
                material.setArchived(1);
                materialMapper.updateById(material);
            }
            if ("S4".equals(stage)) {
                person.setCurrentStage("S5");
                person.setStatus("ARCHIVED");
                personMapper.updateById(person);
            }
            auditService.log(principal, "ARCHIVE_STAGE", "PERSON", personId, "SUCCESS",
                    "归档阶段 " + stage, "archiveNo=" + archiveNo);
            return toRecordVO(record);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ErrorCode.FILE_STORAGE_ERROR, "归档失败: " + e.getMessage());
        }
    }

    public List<ArchiveRecordVO> archives(Long personId, UserPrincipal principal) {
        personService.getEntity(personId, principal);
        return archiveRecordMapper.selectList(new LambdaQueryWrapper<ArchiveRecord>()
                        .eq(ArchiveRecord::getPersonId, personId)
                        .orderByDesc(ArchiveRecord::getArchiveId))
                .stream().map(this::toRecordVO).toList();
    }

    public Path download(Long archiveId, UserPrincipal principal) {
        ArchiveRecord record = archiveRecordMapper.selectById(archiveId);
        if (record == null) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "归档记录不存在");
        }
        personService.getEntity(record.getPersonId(), principal);
        if (!"READY".equals(record.getStatus())) {
            throw new BizException(ErrorCode.STATE_NOT_ALLOWED, "归档包尚未生成完成");
        }
        Path path = fileStorageService.resolve(record.getFilePath());
        if (!Files.exists(path)) {
            throw new BizException(ErrorCode.FILE_STORAGE_ERROR, "归档包文件不存在");
        }
        try {
            ExportLog log = new ExportLog();
            log.setUserId(principal.getUserId());
            log.setExportType("ARCHIVE");
            log.setFileName(record.getArchiveNo() + ".zip");
            log.setFilePath(record.getFilePath());
            log.setWatermarkText("姓名+用途+时间（归档包内 watermark.txt）");
            log.setRowCount(null);
            exportLogMapper.insert(log);
        } catch (Exception ignored) {
        }
        return path;
    }

    public PageResult<SearchResultVO> search(String keyword, long page, long size, UserPrincipal principal) {
        if (keyword == null || keyword.isBlank()) {
            return new PageResult<>(page, size, 0, List.of());
        }
        List<Long> personIds = personIds(principal);
        if (personIds.isEmpty()) {
            return new PageResult<>(page, size, 0, List.of());
        }
        List<Long> materialIds = materialMapper.selectList(new LambdaQueryWrapper<Material>()
                        .in(Material::getPersonId, personIds))
                .stream().map(Material::getMaterialId).toList();
        if (materialIds.isEmpty()) {
            return new PageResult<>(page, size, 0, List.of());
        }
        Page<MaterialVersion> p = new Page<>(page, size);
        Page<MaterialVersion> result = versionMapper.selectPage(p, new LambdaQueryWrapper<MaterialVersion>()
                .in(MaterialVersion::getMaterialId, materialIds)
                .like(MaterialVersion::getTextContent, keyword)
                .orderByDesc(MaterialVersion::getVersionId));
        List<SearchResultVO> items = result.getRecords().stream().map(v -> {
            Material material = materialMapper.selectById(v.getMaterialId());
            Person person = material == null ? null : personMapper.selectById(material.getPersonId());
            MaterialType type = material == null ? null : materialTypeMapper.selectById(material.getMaterialTypeId());
            SearchResultVO vo = new SearchResultVO();
            vo.setVersionId(v.getVersionId());
            vo.setMaterialId(v.getMaterialId());
            vo.setFileName(v.getFileName());
            vo.setUploadedAt(v.getUploadedAt());
            vo.setSnippet(SnippetUtil.snippet(v.getTextContent(), keyword));
            if (material != null) {
                vo.setStage(material.getStage());
                vo.setMaterialTypeName(type == null ? "" : type.getTypeName());
                vo.setPersonId(material.getPersonId());
            }
            if (person != null) {
                vo.setPersonName(person.getName());
                vo.setStudentNo(person.getStudentNo());
            }
            return vo;
        }).toList();
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), items);
    }

    private ArchiveStageVO toStageVO(Person person, String stage, Long templateId) {
        List<ChecklistItem> items = items(templateId, stage);
        List<Material> materials = materials(person.getPersonId(), stage);
        Map<Long, Material> materialMap = materials.stream()
                .collect(Collectors.toMap(Material::getMaterialTypeId, m -> m, (a, b) -> a));
        Map<Long, MaterialType> typeMap = materialTypeMapper.selectList(null).stream()
                .collect(Collectors.toMap(MaterialType::getTypeId, t -> t));
        List<ArchiveMaterialVO> list = new ArrayList<>();
        int required = 0, passed = 0;
        for (ChecklistItem item : items) {
            if (item.getRequired() != null && item.getRequired() == 1) required++;
            Material material = materialMap.get(item.getMaterialTypeId());
            ArchiveMaterialVO vo = new ArchiveMaterialVO();
            vo.setMaterialId(material == null ? null : material.getMaterialId());
            vo.setMaterialTypeId(item.getMaterialTypeId());
            MaterialType type = typeMap.get(item.getMaterialTypeId());
            vo.setMaterialTypeName(type == null ? "" : type.getTypeName());
            vo.setRequired(item.getRequired() != null && item.getRequired() == 1 ? "必填" : "选填");
            vo.setStatus(material == null ? "M1" : material.getStatus());
            vo.setArchived(material != null && material.getArchived() != null && material.getArchived() == 1);
            if (material != null) {
                List<MaterialVersion> versions = versionMapper.selectList(new LambdaQueryWrapper<MaterialVersion>()
                        .eq(MaterialVersion::getMaterialId, material.getMaterialId())
                        .orderByDesc(MaterialVersion::getVersionNo));
                vo.setVersionCount(versions.size());
                if (!versions.isEmpty()) {
                    vo.setLatestVersionNo(versions.get(0).getVersionNo());
                    vo.setLatestFileName(versions.get(0).getFileName());
                }
                if (List.of("M4", "M6").contains(material.getStatus())) passed++;
            }
            list.add(vo);
        }
        ArchiveStageVO stageVO = new ArchiveStageVO();
        stageVO.setStage(stage);
        stageVO.setRequiredCount(required);
        stageVO.setPassedCount(passed);
        stageVO.setComplete(issuesForStage(person, stage, templateId).isEmpty());
        stageVO.setMaterials(list);
        return stageVO;
    }

    private List<String> issuesForStage(Person person, String stage, Long templateId) {
        List<String> issues = new ArrayList<>();
        List<ChecklistItem> items = items(templateId, stage);
        List<Material> materials = materials(person.getPersonId(), stage);
        Map<Long, Material> materialMap = materials.stream()
                .collect(Collectors.toMap(Material::getMaterialTypeId, m -> m, (a, b) -> a));
        Map<Long, MaterialType> typeMap = materialTypeMapper.selectList(null).stream()
                .collect(Collectors.toMap(MaterialType::getTypeId, t -> t));
        for (ChecklistItem item : items) {
            if (item.getRequired() == null || item.getRequired() != 1) continue;
            Material material = materialMap.get(item.getMaterialTypeId());
            String name = typeMap.containsKey(item.getMaterialTypeId())
                    ? typeMap.get(item.getMaterialTypeId()).getTypeName() : ("材料#" + item.getMaterialTypeId());
            if (material == null) {
                issues.add("缺少材料：" + name);
                continue;
            }
            if (!List.of("M4", "M6").contains(material.getStatus())) {
                issues.add("材料未通过：" + name + "（当前 " + material.getStatus() + "）");
                continue;
            }
            Long count = versionMapper.selectCount(new LambdaQueryWrapper<MaterialVersion>()
                    .eq(MaterialVersion::getMaterialId, material.getMaterialId()));
            if (item.getMinCount() != null && count != null && count < item.getMinCount()) {
                issues.add("份数不足：" + name);
            }
        }
        if (items.stream().noneMatch(i -> i.getRequired() != null && i.getRequired() == 1)) {
            issues.add("该阶段未配置必填材料");
        }
        return issues;
    }

    private String buildZip(Path zip, Person person, String stage, List<Material> materials,
                            Map<Long, MaterialType> typeMap, UserPrincipal principal) throws Exception {
        List<Map<String, Object>> manifest = new ArrayList<>();
        try (OutputStream fos = Files.newOutputStream(zip); ZipOutputStream zos = new ZipOutputStream(fos)) {
            for (Material material : materials) {
                MaterialType type = typeMap.get(material.getMaterialTypeId());
                String typeName = type == null ? ("材料" + material.getMaterialTypeId()) : type.getTypeName();
                List<MaterialVersion> versions = versionMapper.selectList(new LambdaQueryWrapper<MaterialVersion>()
                        .eq(MaterialVersion::getMaterialId, material.getMaterialId())
                        .orderByAsc(MaterialVersion::getVersionNo));
                for (MaterialVersion version : versions) {
                    Path file = fileStorageService.resolve(version.getFilePath());
                    if (!Files.exists(file)) {
                        throw new BizException(ErrorCode.FILE_STORAGE_ERROR,
                                "材料文件缺失：" + typeName + " v" + version.getVersionNo());
                    }
                    String entryName = "materials/" + safe(typeName) + "/v" + version.getVersionNo()
                            + "-" + safe(version.getFileName());
                    zos.putNextEntry(new ZipEntry(entryName));
                    Files.copy(file, zos);
                    zos.closeEntry();
                    manifest.add(Map.of(
                            "materialId", material.getMaterialId(),
                            "materialType", typeName,
                            "versionId", version.getVersionId(),
                            "versionNo", version.getVersionNo(),
                            "fileName", version.getFileName(),
                            "sha256", version.getSha256()
                    ));
                }
            }
            String manifestJson = objectMapper.writeValueAsString(manifest);
            zos.putNextEntry(new ZipEntry("manifest.json"));
            zos.write(manifestJson.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            String watermark = "姓名：" + person.getName() + "；学号：" + person.getStudentNo()
                    + "；用途：电子档案归档；阶段：" + stage + "；时间：" + LocalDateTime.now()
                    + "；操作人：" + principal.getRealName();
            zos.putNextEntry(new ZipEntry("watermark.txt"));
            zos.write(watermark.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            return manifestJson;
        }
    }

    private List<ChecklistItem> items(Long templateId, String stage) {
        if (templateId == null) return List.of();
        return checklistItemMapper.selectList(new LambdaQueryWrapper<ChecklistItem>()
                .eq(ChecklistItem::getTemplateId, templateId)
                .eq(ChecklistItem::getStage, stage)
                .orderByAsc(ChecklistItem::getSortNo));
    }

    private List<Material> materials(Long personId, String stage) {
        return materialMapper.selectList(new LambdaQueryWrapper<Material>()
                .eq(Material::getPersonId, personId)
                .eq(Material::getStage, stage));
    }

    private Long templateId(Person person) {
        if (person.getTemplateId() != null) return person.getTemplateId();
        ChecklistTemplate template = checklistService.getPublishedTemplate();
        return template == null ? null : template.getTemplateId();
    }

    private List<Long> personIds(UserPrincipal principal) {
        if ("R1".equals(principal.getRoleCode())) {
            return personMapper.selectList(null).stream().map(Person::getPersonId).toList();
        }
        if (List.of("R2", "R3").contains(principal.getRoleCode())) {
            return personMapper.selectList(new LambdaQueryWrapper<Person>()
                            .eq(Person::getBranchId, principal.getBranchId()))
                    .stream().map(Person::getPersonId).toList();
        }
        if ("R4".equals(principal.getRoleCode()) && principal.getPersonId() != null) {
            return List.of(principal.getPersonId());
        }
        return List.of();
    }

    private ArchiveRecordVO toRecordVO(ArchiveRecord record) {
        ArchiveRecordVO vo = new ArchiveRecordVO();
        vo.setArchiveId(record.getArchiveId());
        vo.setPersonId(record.getPersonId());
        vo.setStage(record.getStage());
        vo.setArchiveNo(record.getArchiveNo());
        vo.setStatus(record.getStatus());
        vo.setArchivedBy(record.getArchivedBy());
        vo.setArchivedAt(record.getArchivedAt());
        if (record.getArchivedBy() != null) {
            SysUser user = userMapper.selectById(record.getArchivedBy());
            vo.setArchivedByName(user == null ? null : user.getRealName());
        }
        try {
            Path path = fileStorageService.resolve(record.getFilePath());
            vo.setFileSize(Files.exists(path) ? Files.size(path) : 0);
        } catch (Exception ignored) {
            vo.setFileSize(0L);
        }
        return vo;
    }

    private String safe(String text) {
        return text == null ? "unknown" : text.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}