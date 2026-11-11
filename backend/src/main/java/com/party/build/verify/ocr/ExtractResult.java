package com.party.build.verify.ocr;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExtractResult {
    private String text;
    private int pageCount;
    private boolean ocrUsed;
    private String engine;
}