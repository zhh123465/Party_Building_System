package com.party.build.verify.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.party.build.common.api.ErrorCode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "app.llm.mock", havingValue = "true")
public class MockLlmClient implements LlmClient {
    private final ObjectMapper objectMapper;

    public MockLlmClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public LlmResponse chat(String systemPrompt, String userPrompt, boolean jsonMode) {
        long start = System.currentTimeMillis();
        try {
            String content = objectMapper.writeValueAsString(Map.of(
                    "findings", List.of(
                            Map.of("point", "入党动机是否具体", "conclusion", "PASS",
                                    "evidence", "结合个人成长经历说明入党动机", "suggestion", "", "confidence", 0.86),
                            Map.of("point", "是否存在空泛套话", "conclusion", "DOUBT",
                                    "evidence", "文中出现较多口号式表述", "suggestion", "补充具体事例", "confidence", 0.72)
                    )
            ));
            LlmResponse r = new LlmResponse();
            r.setRaw("{\"mock\":true}");
            r.setContent(content);
            r.setParsed(objectMapper.readTree(content));
            r.setValidJson(true);
            r.setLatencyMs(System.currentTimeMillis() - start);
            r.setModel("mock-ds1017");
            r.setPromptTokens(0);
            r.setCompletionTokens(0);
            return r;
        } catch (Exception e) {
            throw new LlmException(ErrorCode.LLM_BAD_RESPONSE, e.getMessage());
        }
    }
}