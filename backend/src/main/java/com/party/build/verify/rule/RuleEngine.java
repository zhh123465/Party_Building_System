package com.party.build.verify.rule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.party.build.verify.domain.VerifyRule;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class RuleEngine {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "(19|20)\\d{2}\\s*[年\\-/\\.]\\s*\\d{1,2}\\s*[月\\-/\\.]\\s*\\d{1,2}");
    private static final List<String> SIGNATURE_WORDS = List.of(
            "本人签名", "申请人", "汇报人", "签字", "签名", "本人");

    public List<Finding> evaluate(String text, List<String> points) {
        List<Finding> findings = new ArrayList<>();
        String plain = text == null ? "" : text.replaceAll("\\s", "");

        if (plain.length() < 100) {
            findings.add(new Finding("RULE", "R-GEN-001", "材料正文完整性", "MISSING", "SOFT",
                    plain.length() == 0 ? "未抽取到文本" : "正文仅 " + plain.length() + " 字",
                    "请补充完整材料内容或上传清晰扫描件", 1.0));
        } else {
            findings.add(new Finding("RULE", "R-GEN-001", "材料正文完整性", "PASS", "SOFT",
                    "正文约 " + plain.length() + " 字", "", 1.0));
        }

        boolean hasSignature = SIGNATURE_WORDS.stream().anyMatch(plain::contains);
        findings.add(new Finding("RULE", "R-GEN-002", "本人署名", hasSignature ? "PASS" : "MISSING", "HARD",
                hasSignature ? "检测到署名相关字样" : "未检测到署名相关字样",
                hasSignature ? "" : "请补充本人签名/申请人署名", 0.9));

        boolean hasDate = DATE_PATTERN.matcher(text == null ? "" : text).find();
        findings.add(new Finding("RULE", "R-GEN-003", "材料日期", hasDate ? "PASS" : "MISSING", "HARD",
                hasDate ? "检测到日期" : "未检测到完整日期",
                hasDate ? "" : "请补充材料日期（如 2026年10月1日）", 0.9));

        return findings;
    }

    public List<Finding> evaluateWithRules(String text, List<VerifyRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return evaluate(text, List.of());
        }
        List<Finding> findings = new ArrayList<>();
        String plain = text == null ? "" : text.replaceAll("\\s", "");
        for (VerifyRule rule : rules) {
            String conclusion = "PASS";
            String evidence = "";
            try {
                JsonNode params = rule.getParams() == null || rule.getParams().isBlank()
                        ? objectMapper.createObjectNode() : objectMapper.readTree(rule.getParams());
                switch (rule.getRuleType()) {
                    case "FILE_PRESENT" -> evidence = "材料已上传";
                    case "KEYWORD_PRESENT" -> {
                        JsonNode keywords = params.path("keywords");
                        boolean hit = false;
                        if (keywords.isArray()) {
                            for (JsonNode k : keywords) {
                                if (plain.contains(k.asText())) {
                                    hit = true;
                                    evidence = "检测到关键词：" + k.asText();
                                    break;
                                }
                            }
                        }
                        if (!hit) {
                            conclusion = "MISSING";
                            evidence = "未检测到要求的关键词";
                        }
                    }
                    case "DATE_PRESENT" -> {
                        boolean hit = DATE_PATTERN.matcher(text == null ? "" : text).find();
                        conclusion = hit ? "PASS" : "MISSING";
                        evidence = hit ? "检测到日期" : "未检测到完整日期";
                    }
                    case "MIN_LENGTH" -> {
                        int min = params.path("minLength").asInt(100);
                        boolean hit = plain.length() >= min;
                        conclusion = hit ? "PASS" : "MISSING";
                        evidence = "正文约 " + plain.length() + " 字，要求不少于 " + min + " 字";
                    }
                    case "FILE_FORMAT" -> evidence = "文件格式由上传校验保证";
                    default -> evidence = "规则类型暂由人工处理";
                }
            } catch (Exception e) {
                conclusion = "ERROR";
                evidence = "规则配置解析失败: " + e.getMessage();
            }
            findings.add(new Finding("RULE", rule.getRuleCode(), rule.getRuleName(), conclusion,
                    rule.getSeverity(), evidence, "MISSING".equals(conclusion) ? "请按材料要求补充" : "",
                    "PASS".equals(conclusion) ? 0.95 : 0.8));
        }
        return findings;
    }
}