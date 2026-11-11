package com.party.build.verify.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.party.build.common.api.ErrorCode;
import com.party.build.common.exception.BizException;
import com.party.build.verify.rule.Finding;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SemanticVerifier {
    private static final int MAX_TEXT = 12000;

    private final LlmClient llmClient;

    public LlmResponse rawChat(String systemPrompt, String userPrompt) {
        LlmResponse first = llmClient.chat(systemPrompt, userPrompt, true);
        if (first.isValidJson()) {
            return first;
        }
        String retryPrompt = userPrompt + "\n\n注意：上一次输出不是合法 JSON。请只输出一个 JSON 对象，不要 Markdown 代码块。";
        return llmClient.chat(systemPrompt, retryPrompt, true);
    }

    public List<Finding> verify(String text, List<String> points) {
        String system = "你是高校党务材料核验助手。只依据原文判断，不臆测；证据必须摘录原文；"
                + "无法判断输出 UNKNOWN。只输出 JSON，不要 Markdown。";
        String user = buildUserPrompt(text, points);
        LlmResponse response = llmClient.chat(system, user, true);
        if (!response.isValidJson()) {
            response = llmClient.chat(system, user + "\n\n上次输出不是合法 JSON，请只输出 JSON。", true);
        }
        if (!response.isValidJson() || response.getParsed() == null) {
            throw new BizException(ErrorCode.LLM_BAD_RESPONSE, "模型返回无法解析为 JSON");
        }
        JsonNode findings = response.getParsed().path("findings");
        if (!findings.isArray()) {
            throw new BizException(ErrorCode.LLM_BAD_RESPONSE, "模型返回缺少 findings 数组");
        }
        List<Finding> result = new ArrayList<>();
        for (JsonNode node : findings) {
            String conclusion = node.path("conclusion").asText("UNKNOWN").toUpperCase();
            if (!List.of("PASS", "DOUBT", "CONFLICT", "UNKNOWN").contains(conclusion)) {
                conclusion = "UNKNOWN";
            }
            result.add(new Finding(
                    "SEMANTIC",
                    node.path("point_code").asText(""),
                    node.path("point").asText("未命名要点"),
                    conclusion,
                    "SOFT",
                    node.path("evidence").asText(""),
                    node.path("suggestion").asText(""),
                    node.path("confidence").asDouble(0.5)
            ));
        }
        return result;
    }

    private String buildUserPrompt(String text, List<String> points) {
        String safeText = text == null ? "" : text;
        if (safeText.length() > MAX_TEXT) {
            safeText = safeText.substring(0, MAX_TEXT) + "\n[文本已截断]";
        }
        String pointText = String.join("\n", points.stream().map(p -> "- " + p).toList());
        return "核验要点：\n" + pointText + "\n\n材料文本：\n" + safeText
                + "\n\n输出格式：{\"findings\":[{\"point\":\"要点\",\"conclusion\":\"PASS|DOUBT|CONFLICT|UNKNOWN\","
                + "\"evidence\":\"原文片段\",\"suggestion\":\"修改建议\",\"confidence\":0.0}]}";
    }
}