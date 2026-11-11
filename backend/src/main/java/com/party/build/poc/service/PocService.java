package com.party.build.poc.service;

import com.party.build.common.exception.BizException;
import com.party.build.poc.dto.OcrPocResponse;
import com.party.build.poc.dto.VerifyPocResponse;
import com.party.build.verify.ai.LlmResponse;
import com.party.build.verify.ai.SemanticVerifier;
import com.party.build.verify.ocr.ExtractResult;
import com.party.build.verify.ocr.TextExtractService;
import com.party.build.verify.rule.Finding;
import com.party.build.verify.rule.RuleEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PocService {
    private final TextExtractService textExtractService;
    private final RuleEngine ruleEngine;
    private final SemanticVerifier semanticVerifier;

    public OcrPocResponse ocr(MultipartFile file) throws IOException {
        long start = System.currentTimeMillis();
        ExtractResult result = textExtractService.extract(file.getInputStream(), file.getOriginalFilename());
        OcrPocResponse response = new OcrPocResponse();
        response.setFileName(file.getOriginalFilename());
        response.setText(result.getText());
        response.setCharCount(result.getText() == null ? 0 : result.getText().length());
        response.setPageCount(result.getPageCount());
        response.setOcrUsed(result.isOcrUsed());
        response.setEngine(result.getEngine());
        response.setLatencyMs(System.currentTimeMillis() - start);
        return response;
    }

    public LlmResponse llm(String systemPrompt, String userPrompt) {
        return semanticVerifier.rawChat(systemPrompt, userPrompt);
    }

    public VerifyPocResponse verify(MultipartFile file, List<String> points) throws IOException {
        long start = System.currentTimeMillis();
        ExtractResult extracted = textExtractService.extract(file.getInputStream(), file.getOriginalFilename());
        List<Finding> ruleFindings = ruleEngine.evaluate(extracted.getText(), points);
        List<Finding> semanticFindings = new ArrayList<>();
        boolean manualRequired = false;
        String error = null;
        try {
            semanticFindings = semanticVerifier.verify(extracted.getText(), points);
        } catch (BizException e) {
            manualRequired = true;
            error = e.getMessage();
            semanticFindings.add(new Finding("SEMANTIC", "", "语义核验", "UNKNOWN", "SOFT",
                    "", "已转人工处理，不阻断主流程", 0.0));
        }

        String overall = overall(ruleFindings, semanticFindings, manualRequired);
        VerifyPocResponse response = new VerifyPocResponse();
        response.setOverall(overall);
        response.setRuleFindings(ruleFindings);
        response.setSemanticFindings(semanticFindings);
        String text = extracted.getText() == null ? "" : extracted.getText();
        response.setTextPreview(text.length() > 500 ? text.substring(0, 500) + "..." : text);
        response.setTextLength(text.length());
        response.setPageCount(extracted.getPageCount());
        response.setOcrUsed(extracted.isOcrUsed());
        response.setEngine(extracted.getEngine());
        response.setLatencyMs(System.currentTimeMillis() - start);
        response.setManualRequired(manualRequired);
        response.setError(error);
        return response;
    }

    private String overall(List<Finding> rules, List<Finding> semantics, boolean manualRequired) {
        boolean hardMissing = rules.stream()
                .anyMatch(f -> "HARD".equals(f.getSeverity()) && "MISSING".equals(f.getConclusion()));
        if (hardMissing) {
            return "NEED_FIX";
        }
        boolean conflict = semantics.stream().anyMatch(f -> "CONFLICT".equals(f.getConclusion()));
        if (conflict) {
            return "NEED_FIX";
        }
        if (manualRequired || semantics.stream().anyMatch(f ->
                "DOUBT".equals(f.getConclusion()) || "UNKNOWN".equals(f.getConclusion()))) {
            return "DOUBT";
        }
        return "PASS";
    }
}