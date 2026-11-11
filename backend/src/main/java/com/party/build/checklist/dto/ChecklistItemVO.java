package com.party.build.checklist.dto;

import lombok.Data;

import java.util.List;

@Data
public class ChecklistItemVO {
    private Long itemId;
    private Long templateId;
    private String stage;
    private Long materialTypeId;
    private String materialTypeName;
    private String materialTypeCode;
    private Integer required;
    private Integer minCount;
    private Integer maxCount;
    private Integer minPages;
    private List<String> verifyPoints;
    private Integer sortNo;
}