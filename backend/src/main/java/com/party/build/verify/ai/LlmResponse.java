package com.party.build.verify.ai;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class LlmResponse {
    private String raw;
    private String content;
    private JsonNode parsed;
    private boolean validJson;
    private long latencyMs;
    private String model;
    private int promptTokens;
    private int completionTokens;
}