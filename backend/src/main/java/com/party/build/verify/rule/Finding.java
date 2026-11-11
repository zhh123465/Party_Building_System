package com.party.build.verify.rule;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Finding {
    private String layer;
    private String ruleCode;
    private String point;
    private String conclusion;
    private String severity;
    private String evidence;
    private String suggestion;
    private Double confidence;
}