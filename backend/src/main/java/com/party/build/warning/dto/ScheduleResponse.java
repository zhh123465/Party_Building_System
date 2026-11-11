package com.party.build.warning.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ScheduleResponse {
    private Long personId;
    private String personName;
    private String targetNode;
    private LocalDate targetDate;
    private boolean feasible;
    private List<ScheduleNodeVO> nodes;
    private List<String> criticalPath;
}