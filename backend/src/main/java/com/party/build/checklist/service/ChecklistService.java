package com.party.build.checklist.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.party.build.checklist.domain.ChecklistItem;
import com.party.build.checklist.domain.ChecklistTemplate;
import com.party.build.checklist.domain.MaterialType;
import com.party.build.checklist.dto.*;
import com.party.build.checklist.mapper.ChecklistItemMapper;
import com.party.build.checklist.mapper.ChecklistTemplateMapper;
import com.party.build.checklist.mapper.MaterialTypeMapper;
import com.party.build.common.api.ErrorCode;
import com.party.build.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChecklistService {
    private static final List<String> STAGES = List.of("S0", "S1", "S2", "S3", "S4");

    private final MaterialTypeMapper materialTypeMapper;
    private final ChecklistTemplateMapper templateMapper;
    private final ChecklistItemMapper itemMapper;
    private final ObjectMapper objectMapper;

    public List<MaterialType> listMaterialTypes() {
        return materialTypeMapper.selectList(new LambdaQueryWrapper<MaterialType>()
                .orderByAsc(MaterialType::getSortNo).orderByAsc(MaterialType::getTypeId));
    }

    public MaterialType createMaterialType(MaterialTypeRequest request) {
        Long count = materialTypeMapper.selectCount(new LambdaQueryWrapper<MaterialType>()
                .eq(MaterialType::getTypeCode, request.getTypeCode()));
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.ALREADY_EXISTS, "材料类型编码已存在");
        }
        MaterialType type = new MaterialType();
        BeanUtils.copyProperties(request, type);
        materialTypeMapper.insert(type);
        return type;
    }

    public MaterialType updateMaterialType(Long id, MaterialTypeRequest request) {
        MaterialType type = materialTypeMapper.selectById(id);
        if (type == null) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "材料类型不存在");
        }
        BeanUtils.copyProperties(request, type);
        type.setTypeId(id);
        materialTypeMapper.updateById(type);
        return type;
    }

    public List<ChecklistTemplate> listTemplates() {
        return templateMapper.selectList(new LambdaQueryWrapper<ChecklistTemplate>()
                .orderByDesc(ChecklistTemplate::getTemplateId));
    }

    public ChecklistTemplateVO getTemplate(Long id) {
        ChecklistTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "清单模板不存在");
        }
        ChecklistTemplateVO vo = new ChecklistTemplateVO();
        BeanUtils.copyProperties(template, vo);
        vo.setItems(listItems(id));
        return vo;
    }

    public List<ChecklistItemVO> listItems(Long templateId) {
        List<ChecklistItem> items = itemMapper.selectList(new LambdaQueryWrapper<ChecklistItem>()
                .eq(ChecklistItem::getTemplateId, templateId)
                .orderByAsc(ChecklistItem::getStage).orderByAsc(ChecklistItem::getSortNo));
        Map<Long, MaterialType> types = materialTypeMapper.selectList(null).stream()
                .collect(Collectors.toMap(MaterialType::getTypeId, t -> t));
        return items.stream().map(item -> toVO(item, types.get(item.getMaterialTypeId()))).toList();
    }

    public ChecklistTemplateVO createTemplate(ChecklistTemplateRequest request) {
        LambdaQueryWrapper<ChecklistTemplate> q = new LambdaQueryWrapper<ChecklistTemplate>()
                .eq(ChecklistTemplate::getTemplateCode, request.getTemplateCode())
                .orderByDesc(ChecklistTemplate::getVersionNo).last("limit 1");
        ChecklistTemplate latest = templateMapper.selectOne(q);
        ChecklistTemplate template = new ChecklistTemplate();
        BeanUtils.copyProperties(request, template);
        template.setVersionNo(latest == null ? 1 : latest.getVersionNo() + 1);
        template.setStatus("DRAFT");
        templateMapper.insert(template);
        ChecklistTemplateVO vo = new ChecklistTemplateVO();
        BeanUtils.copyProperties(template, vo);
        vo.setItems(List.of());
        return vo;
    }

    public ChecklistTemplateVO updateTemplate(Long id, ChecklistTemplateRequest request) {
        ChecklistTemplate template = requireDraft(id);
        template.setTemplateName(request.getTemplateName());
        template.setAcademicYearId(request.getAcademicYearId());
        template.setEffectiveFrom(request.getEffectiveFrom());
        template.setEffectiveTo(request.getEffectiveTo());
        templateMapper.updateById(template);
        return getTemplate(id);
    }

    public ChecklistTemplateVO cloneTemplate(Long id) {
        ChecklistTemplate source = templateMapper.selectById(id);
        if (source == null) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "清单模板不存在");
        }
        ChecklistTemplate copy = new ChecklistTemplate();
        BeanUtils.copyProperties(source, copy);
        copy.setTemplateId(null);
        copy.setVersionNo(source.getVersionNo() + 1);
        copy.setStatus("DRAFT");
        templateMapper.insert(copy);
        for (ChecklistItem item : itemMapper.selectList(new LambdaQueryWrapper<ChecklistItem>()
                .eq(ChecklistItem::getTemplateId, id))) {
            ChecklistItem newItem = new ChecklistItem();
            BeanUtils.copyProperties(item, newItem);
            newItem.setItemId(null);
            newItem.setTemplateId(copy.getTemplateId());
            itemMapper.insert(newItem);
        }
        return getTemplate(copy.getTemplateId());
    }

    public ChecklistItemVO addItem(Long templateId, ChecklistItemRequest request) {
        requireDraft(templateId);
        MaterialType type = requireType(request.getMaterialTypeId());
        ChecklistItem item = new ChecklistItem();
        BeanUtils.copyProperties(request, item);
        item.setTemplateId(templateId);
        item.setVerifyPoints(writePoints(request.getVerifyPoints()));
        itemMapper.insert(item);
        return toVO(item, type);
    }

    public ChecklistItemVO updateItem(Long itemId, ChecklistItemRequest request) {
        ChecklistItem item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "清单条目不存在");
        }
        requireDraft(item.getTemplateId());
        MaterialType type = requireType(request.getMaterialTypeId());
        BeanUtils.copyProperties(request, item);
        item.setItemId(itemId);
        item.setVerifyPoints(writePoints(request.getVerifyPoints()));
        itemMapper.updateById(item);
        return toVO(item, type);
    }

    public void deleteItem(Long itemId) {
        ChecklistItem item = itemMapper.selectById(itemId);
        if (item == null) {
            return;
        }
        requireDraft(item.getTemplateId());
        itemMapper.deleteById(itemId);
    }

    public TemplateValidateResponse validate(Long templateId) {
        ChecklistTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "清单模板不存在");
        }
        List<ChecklistItem> items = itemMapper.selectList(new LambdaQueryWrapper<ChecklistItem>()
                .eq(ChecklistItem::getTemplateId, templateId));
        List<String> errors = new ArrayList<>();
        if (items.isEmpty()) {
            errors.add("模板至少需要一条材料条目");
        }
        Set<String> stages = items.stream().map(ChecklistItem::getStage).collect(Collectors.toSet());
        for (String stage : STAGES) {
            if (!stages.contains(stage)) {
                errors.add("阶段 " + stage + " 缺少材料条目");
            }
        }
        Set<Long> typeIds = materialTypeMapper.selectList(null).stream()
                .map(MaterialType::getTypeId).collect(Collectors.toSet());
        for (ChecklistItem item : items) {
            if (!typeIds.contains(item.getMaterialTypeId())) {
                errors.add("条目引用了不存在的材料类型: " + item.getMaterialTypeId());
            }
        }
        return new TemplateValidateResponse(errors.isEmpty(), errors);
    }

    @Transactional
    public ChecklistTemplateVO publish(Long templateId) {
        TemplateValidateResponse check = validate(templateId);
        if (!check.isValid()) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, String.join("; ", check.getErrors()));
        }
        ChecklistTemplate template = templateMapper.selectById(templateId);
        // 同一学年版只保留一个已发布模板
        for (ChecklistTemplate other : templateMapper.selectList(new LambdaQueryWrapper<ChecklistTemplate>()
                .eq(ChecklistTemplate::getAcademicYearId, template.getAcademicYearId())
                .eq(ChecklistTemplate::getStatus, "PUBLISHED"))) {
            if (!other.getTemplateId().equals(templateId)) {
                other.setStatus("ARCHIVED");
                templateMapper.updateById(other);
            }
        }
        template.setStatus("PUBLISHED");
        templateMapper.updateById(template);
        return getTemplate(templateId);
    }

    public ChecklistTemplate getPublishedTemplate() {
        return templateMapper.selectOne(new LambdaQueryWrapper<ChecklistTemplate>()
                .eq(ChecklistTemplate::getStatus, "PUBLISHED")
                .orderByDesc(ChecklistTemplate::getVersionNo).last("limit 1"));
    }

    private ChecklistTemplate requireDraft(Long templateId) {
        ChecklistTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "清单模板不存在");
        }
        if (!"DRAFT".equals(template.getStatus())) {
            throw new BizException(ErrorCode.STATE_NOT_ALLOWED, "只有草稿模板可以修改");
        }
        return template;
    }

    private MaterialType requireType(Long typeId) {
        MaterialType type = materialTypeMapper.selectById(typeId);
        if (type == null) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "材料类型不存在");
        }
        return type;
    }

    private ChecklistItemVO toVO(ChecklistItem item, MaterialType type) {
        ChecklistItemVO vo = new ChecklistItemVO();
        BeanUtils.copyProperties(item, vo);
        if (type != null) {
            vo.setMaterialTypeName(type.getTypeName());
            vo.setMaterialTypeCode(type.getTypeCode());
        }
        vo.setVerifyPoints(readPoints(item.getVerifyPoints()));
        return vo;
    }

    private String writePoints(List<String> points) {
        try {
            return objectMapper.writeValueAsString(points == null ? List.of() : points);
        } catch (Exception e) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "核验要点格式错误");
        }
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