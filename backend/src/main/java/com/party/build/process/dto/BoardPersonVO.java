package com.party.build.process.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class BoardPersonVO {
    private Long personId;
    private String name;
    private String studentNo;
    private Long branchId;
    private String currentStage;
    private LocalDate stageStartDate;
    private int requiredCount;
    private int passedCount;
    private int percent;
    private String riskLevel;
    private boolean locked;
    private int blockerCount;
    private int overdueCount;
    private int returnedCount;
    private List<String> blockers;
}