package com.party.build.verify.ai;

public interface LlmClient {
    LlmResponse chat(String systemPrompt, String userPrompt, boolean jsonMode);
}