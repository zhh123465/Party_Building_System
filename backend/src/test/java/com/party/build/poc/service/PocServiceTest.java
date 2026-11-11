package com.party.build.poc.service;

import com.party.build.common.api.ErrorCode;
import com.party.build.common.exception.BizException;
import com.party.build.poc.dto.VerifyPocResponse;
import com.party.build.verify.ai.SemanticVerifier;
import com.party.build.verify.ocr.ExtractResult;
import com.party.build.verify.ocr.TextExtractService;
import com.party.build.verify.rule.Finding;
import com.party.build.verify.rule.RuleEngine;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PocServiceTest {

    @Test
    void shouldDegradeWhenModelTimesOut() throws Exception {
        TextExtractService extractor = mock(TextExtractService.class);
        RuleEngine rules = mock(RuleEngine.class);
        SemanticVerifier semantic = mock(SemanticVerifier.class);
        when(extractor.extract(any(), any())).thenReturn(new ExtractResult("申请人：张明 2026年9月10日", 1, false, "test"));
        when(rules.evaluate(any(), anyList())).thenReturn(List.of(
                new Finding("RULE", "R-GEN-002", "本人署名", "PASS", "HARD", "检测到署名", "", 0.9)
        ));
        when(semantic.verify(any(), anyList())).thenThrow(new BizException(ErrorCode.LLM_TIMEOUT, "模型调用超时"));

        PocService service = new PocService(extractor, rules, semantic);
        MockMultipartFile file = new MockMultipartFile("file", "a.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "x".getBytes());
        VerifyPocResponse response = service.verify(file, List.of("入党动机"));

        assertThat(response.isManualRequired()).isTrue();
        assertThat(response.getError()).contains("超时");
        assertThat(response.getSemanticFindings()).anyMatch(f -> "UNKNOWN".equals(f.getConclusion()));
        assertThat(response.getOverall()).isEqualTo("DOUBT");
    }

    @Test
    void shouldPassWhenRulesAndSemanticsPass() throws Exception {
        TextExtractService extractor = mock(TextExtractService.class);
        RuleEngine rules = mock(RuleEngine.class);
        SemanticVerifier semantic = mock(SemanticVerifier.class);
        when(extractor.extract(any(), any())).thenReturn(new ExtractResult("材料文本", 1, false, "test"));
        when(rules.evaluate(any(), anyList())).thenReturn(List.of(
                new Finding("RULE", "R-GEN-002", "本人署名", "PASS", "HARD", "检测到署名", "", 0.9)
        ));
        when(semantic.verify(any(), anyList())).thenReturn(List.of(
                new Finding("SEMANTIC", "", "入党动机", "PASS", "SOFT", "结合经历", "", 0.9)
        ));

        PocService service = new PocService(extractor, rules, semantic);
        MockMultipartFile file = new MockMultipartFile("file", "a.docx", "application/octet-stream", "x".getBytes());
        VerifyPocResponse response = service.verify(file, List.of("入党动机"));
        assertThat(response.isManualRequired()).isFalse();
        assertThat(response.getOverall()).isEqualTo("PASS");
    }
}