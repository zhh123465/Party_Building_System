package com.party.build.poc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcrPocResponse {
    private String fileName;
    private String text;
    private int charCount;
    private int pageCount;
    private boolean ocrUsed;
    private String engine;
    private long latencyMs;
}