package com.party.build.warning.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ScheduleNodeVO {
    private String ruleCode;
    private String nodeName;
    private String stage;
    private LocalDate latestStart;
    private LocalDate latestDue;
    private String status;
    private String dependsOn;
}