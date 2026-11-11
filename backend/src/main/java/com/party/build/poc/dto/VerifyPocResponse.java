package com.party.build.poc.dto;

import com.party.build.verify.rule.Finding;
import lombok.Data;

import java.util.List;

@Data
public class VerifyPocResponse {
    private String overall;
    private List<Finding> ruleFindings;
    private List<Finding> semanticFindings;
    private String textPreview;
    private int textLength;
    private int pageCount;
    private boolean ocrUsed;
    private String engine;
    private long latencyMs;
    private boolean manualRequired;
    private String error;
}