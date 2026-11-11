package com.party.build.process.dto;

import lombok.Data;

import java.util.List;

@Data
public class AdvanceCheckVO {
    private Long personId;
    private String personName;
    private String currentStage;
    private String targetStage;
    private boolean passed;
    private List<Blocker> blockers;
    private List<String> warnings;
    private List<String> requiredFields;
    private MaterialSummary materialSummary;
}
