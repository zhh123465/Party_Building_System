package com.party.build.checklist.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ChecklistTemplateVO {
    private Long templateId;
    private String templateCode;
    private String templateName;
    private Long academicYearId;
    private Integer versionNo;
    private String status;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private LocalDateTime createdAt;
    private List<ChecklistItemVO> items;
}