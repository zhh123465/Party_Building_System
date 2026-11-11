package com.party.build.verify.rule;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class RuleEngine {
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
}