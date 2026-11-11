package com.party.build.poc.controller;

import com.party.build.common.api.Result;
import com.party.build.poc.dto.LlmPocRequest;
import com.party.build.poc.dto.OcrPocResponse;
import com.party.build.poc.dto.VerifyPocResponse;
import com.party.build.poc.service.PocService;
import com.party.build.verify.ai.LlmResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/poc")
@RequiredArgsConstructor
public class PocController {
    private static final List<String> DEFAULT_POINTS = List.of(
            "入党动机是否具体",
            "是否结合个人经历",
            "是否存在空泛套话",
            "思想变化是否真实"
    );

    private final PocService pocService;

    @Value("${app.llm.provider:tokenrhythm}")
    private String provider;
    @Value("${app.llm.base-url}")
    private String baseUrl;
    @Value("${app.llm.model}")
    private String model;
    @Value("${app.llm.mock:false}")
    private boolean mock;
    @Value("${app.llm.timeout-seconds:30}")
    private long timeoutSeconds;

    @GetMapping("/config")
    public Result<Map<String, Object>> config() {
        return Result.ok(Map.of(
                "provider", provider,
                "baseUrl", baseUrl,
                "model", model,
                "mock", mock,
                "timeoutSeconds", timeoutSeconds,
                "ocr", "tesseract chi_sim+eng"
        ));
    }

    @PostMapping("/llm")
    public Result<LlmResponse> llm(@Valid @RequestBody LlmPocRequest request) {
        return Result.ok(pocService.llm(request.getSystemPrompt(), request.getUserPrompt()));
    }

    @PostMapping("/ocr")
    public Result<OcrPocResponse> ocr(@RequestParam("file") MultipartFile file) throws IOException {
        return Result.ok(pocService.ocr(file));
    }

    @PostMapping("/verify")
    public Result<VerifyPocResponse> verify(@RequestParam("file") MultipartFile file,
                                            @RequestParam(value = "points", required = false) String points)
            throws IOException {
        return Result.ok(pocService.verify(file, parsePoints(points)));
    }

    private List<String> parsePoints(String points) {
        if (points == null || points.isBlank()) {
            return DEFAULT_POINTS;
        }
        List<String> list = Arrays.stream(points.split("[,，;；\\n]"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
        return list.isEmpty() ? DEFAULT_POINTS : list;
    }
}