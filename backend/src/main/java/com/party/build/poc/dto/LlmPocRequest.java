package com.party.build.poc.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LlmPocRequest {
    private String systemPrompt = "你是高校党务材料核验助手，只输出 JSON。";
    @NotBlank
    private String userPrompt;
}