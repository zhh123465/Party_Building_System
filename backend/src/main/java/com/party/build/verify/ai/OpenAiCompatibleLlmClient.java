package com.party.build.verify.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.party.build.common.api.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.llm.mock", havingValue = "false", matchIfMissing = true)
public class OpenAiCompatibleLlmClient implements LlmClient {
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final Duration timeout;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiCompatibleLlmClient(@Value("${app.llm.base-url}") String baseUrl,
                                     @Value("${app.llm.api-key}") String apiKey,
                                     @Value("${app.llm.model}") String model,
                                     @Value("${app.llm.timeout-seconds:30}") long timeoutSeconds,
                                     ObjectMapper objectMapper) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public LlmResponse chat(String systemPrompt, String userPrompt, boolean jsonMode) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new LlmException(ErrorCode.LLM_BAD_RESPONSE, "LLM_API_KEY 未配置");
        }
        long start = System.currentTimeMillis();
        try {
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("model", model);
            body.put("temperature", 0);
            body.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
            ));
            if (jsonMode) {
                body.put("response_format", Map.of("type", "json_object"));
            }
            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .timeout(timeout)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long latency = System.currentTimeMillis() - start;
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new LlmException(ErrorCode.LLM_BAD_RESPONSE,
                        "模型接口返回 HTTP " + response.statusCode() + ": " + truncate(response.body(), 300));
            }
            return parseResponse(response.body(), latency);
        } catch (HttpTimeoutException e) {
            throw new LlmException(ErrorCode.LLM_TIMEOUT, "模型调用超时（" + timeout.toSeconds() + " 秒）");
        } catch (LlmException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException(ErrorCode.UNKNOWN, "模型调用被中断");
        } catch (Exception e) {
            log.warn("llm call failed", e);
            throw new LlmException(ErrorCode.LLM_BAD_RESPONSE, "模型调用失败: " + e.getMessage());
        }
    }

    private LlmResponse parseResponse(String raw, long latency) throws Exception {
        JsonNode root = objectMapper.readTree(raw);
        JsonNode choice = root.path("choices").path(0).path("message");
        String content = choice.path("content").asText("");
        LlmResponse result = new LlmResponse();
        result.setRaw(raw);
        result.setContent(content);
        result.setLatencyMs(latency);
        result.setModel(root.path("model").asText(model));
        result.setPromptTokens(root.path("usage").path("prompt_tokens").asInt(0));
        result.setCompletionTokens(root.path("usage").path("completion_tokens").asInt(0));
        String json = extractJson(content);
        try {
            result.setParsed(objectMapper.readTree(json));
            result.setValidJson(true);
        } catch (Exception e) {
            result.setValidJson(false);
        }
        return result;
    }

    private String extractJson(String content) {
        if (content == null) {
            return "";
        }
        String text = content.trim();
        if (text.startsWith("```")) {
            int first = text.indexOf('\n');
            int last = text.lastIndexOf("```");
            if (first >= 0 && last > first) {
                text = text.substring(first + 1, last).trim();
            }
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }
}