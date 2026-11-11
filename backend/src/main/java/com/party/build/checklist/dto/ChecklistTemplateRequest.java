package com.party.build.checklist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ChecklistTemplateRequest {
    @NotBlank
    private String templateCode;
    @NotBlank
    private String templateName;
    @NotNull
    private Long academicYearId;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
}