package com.party.build.verify.rule;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEngineTest {
    private final RuleEngine engine = new RuleEngine();

    @Test
    void shouldPassCompleteApplication() {
        String text = "敬爱的党组织：我志愿加入中国共产党。".repeat(10)
                + "申请人：张明 2026年9月10日";
        List<Finding> findings = engine.evaluate(text, List.of("入党动机"));
        assertThat(findings).anyMatch(f -> "R-GEN-002".equals(f.getRuleCode()) && "PASS".equals(f.getConclusion()));
        assertThat(findings).anyMatch(f -> "R-GEN-003".equals(f.getRuleCode()) && "PASS".equals(f.getConclusion()));
    }

    @Test
    void shouldMarkMissingSignatureAndDate() {
        List<Finding> findings = engine.evaluate("很短的一段文字", List.of("入党动机"));
        assertThat(findings).anyMatch(f -> "R-GEN-001".equals(f.getRuleCode()) && "MISSING".equals(f.getConclusion()));
        assertThat(findings).anyMatch(f -> "R-GEN-002".equals(f.getRuleCode()) && "MISSING".equals(f.getConclusion()));
        assertThat(findings).anyMatch(f -> "R-GEN-003".equals(f.getRuleCode()) && "MISSING".equals(f.getConclusion()));
    }
}