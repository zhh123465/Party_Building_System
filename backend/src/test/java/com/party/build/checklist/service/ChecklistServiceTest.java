package com.party.build.checklist.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.party.build.checklist.domain.ChecklistItem;
import com.party.build.checklist.domain.ChecklistTemplate;
import com.party.build.checklist.domain.MaterialType;
import com.party.build.checklist.dto.TemplateValidateResponse;
import com.party.build.checklist.mapper.ChecklistItemMapper;
import com.party.build.checklist.mapper.ChecklistTemplateMapper;
import com.party.build.checklist.mapper.MaterialTypeMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChecklistServiceTest {

    private ChecklistService service(List<ChecklistItem> items) {
        MaterialTypeMapper typeMapper = mock(MaterialTypeMapper.class);
        ChecklistTemplateMapper templateMapper = mock(ChecklistTemplateMapper.class);
        ChecklistItemMapper itemMapper = mock(ChecklistItemMapper.class);

        MaterialType type = new MaterialType();
        type.setTypeId(1L);
        type.setTypeName("入党申请书");
        when(typeMapper.selectList(any())).thenReturn(List.of(type));
        when(templateMapper.selectById(1L)).thenReturn(template());
        when(itemMapper.selectList(any())).thenReturn(items);

        return new ChecklistService(typeMapper, templateMapper, itemMapper, new ObjectMapper());
    }

    private ChecklistTemplate template() {
        ChecklistTemplate t = new ChecklistTemplate();
        t.setTemplateId(1L);
        t.setTemplateCode("DEFAULT");
        t.setAcademicYearId(1L);
        t.setVersionNo(1);
        t.setStatus("DRAFT");
        return t;
    }

    private ChecklistItem item(String stage) {
        ChecklistItem i = new ChecklistItem();
        i.setItemId(1L);
        i.setTemplateId(1L);
        i.setStage(stage);
        i.setMaterialTypeId(1L);
        return i;
    }

    @Test
    void shouldPassWhenAllStagesCovered() {
        ChecklistService service = service(List.of(
                item("S0"), item("S1"), item("S2"), item("S3"), item("S4")
        ));
        TemplateValidateResponse result = service.validate(1L);
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void shouldFailWhenStageMissing() {
        ChecklistService service = service(List.of(item("S0")));
        TemplateValidateResponse result = service.validate(1L);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(s -> s.contains("S1"));
    }
}