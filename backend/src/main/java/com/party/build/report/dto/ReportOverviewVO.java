package com.party.build.report.dto;

import lombok.Data;

import java.util.List;

@Data
public class ReportOverviewVO {
    private long totalPersons;
    private long activePersons;
    private int totalRequired;
    private int totalPassed;
    private double completionRate;
    private long overdueWarnings;
    private double avgDaysInStage;
    private double averageCompliance;
    private List<StageCountVO> stageCounts;
}
