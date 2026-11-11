package com.party.build.verify.ai;

import com.party.build.common.api.ErrorCode;
import com.party.build.common.exception.BizException;
import com.party.build.verify.rule.Finding;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SemanticVerifierTest {
    private LlmResponse response(String content, boolean valid) {
        LlmResponse r = new LlmResponse();
        r.setContent(content);
        r.setValidJson(valid);
        try {
            r.setParsed(valid ? new com.fasterxml.jackson.databind.ObjectMapper().readTree(content) : null);
        } catch (Exception e) {
            r.setValidJson(false);
        }
        return r;
    }

    @Test
    void shouldMapFindings() throws Exception {
        String json = "{\"findings\":[{\"point\":\"入党动机\",\"conclusion\":\"PASS\",\"evidence\":\"结合经历\",\"confidence\":0.9}]}";
        LlmClient client = (s, u, j) -> response(json, true);
        List<Finding> findings = new SemanticVerifier(client).verify("材料文本", List.of("入党动机"));
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getConclusion()).isEqualTo("PASS");
        assertThat(findings.get(0).getLayer()).isEqualTo("SEMANTIC");
    }

    @Test
    void shouldThrowWhenModelKeepsReturningInvalidJson() throws Exception {
        LlmClient client = (s, u, j) -> response("not json", false);
        assertThatThrownBy(() -> new SemanticVerifier(client).verify("材料文本", List.of("入党动机")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("JSON");
    }

    @Test
    void shouldDefaultUnknownConclusion() throws Exception {
        String json = "{\"findings\":[{\"point\":\"入党动机\",\"conclusion\":\"WHATEVER\",\"evidence\":\"\"}]}";
        LlmClient client = (s, u, j) -> response(json, true);
        List<Finding> findings = new SemanticVerifier(client).verify("材料文本", List.of("入党动机"));
        assertThat(findings.get(0).getConclusion()).isEqualTo("UNKNOWN");
    }
}