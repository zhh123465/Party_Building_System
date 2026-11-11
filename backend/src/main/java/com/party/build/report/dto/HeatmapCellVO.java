package com.party.build.report.dto;

import lombok.Data;

@Data
public class HeatmapCellVO {
    private Long branchId;
    private String branchName;
    private String stage;
    private double avgScore;
    private long count;
    private String riskLevel;
}