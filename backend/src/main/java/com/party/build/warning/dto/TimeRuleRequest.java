package com.party.build.warning.dto;

import lombok.Data;

import java.util.List;

@Data
public class TimeRuleRequest {
    private String nodeName;
    private String stage;
    private Integer offsetValue;
    private String offsetUnit;
    private List<Integer> warnOffsets;
    private String responsibleRole;
    private Integer enabled;
}