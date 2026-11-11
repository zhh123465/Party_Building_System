package com.party.build.report.dto;

import lombok.Data;

@Data
public class ComplianceVO {
    private Long personId;
    private String name;
    private String studentNo;
    private Long branchId;
    private String branchName;
    private String stage;
    private int required;
    private int passed;
    private double materialScore;
    private double timeScore;
    private double contentScore;
    private double totalScore;
    private String riskLevel;
    private int overdue;
    private int urgent;
    private int near;
    private int info;
    private int conflict;
    private int doubt;
    private int lowConfidence;
    private int semanticTotal;
}