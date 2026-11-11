package com.party.build.process.dto;

import lombok.Data;

import java.util.List;

@Data
public class MaterialSummary {
    private int requiredCount;
    private int passedCount;
    private int missingCount;
    private int returnedCount;
    private int overdueCount;
    private List<String> issues;
    private List<String> previousStageIssues;
    private int percent;
}