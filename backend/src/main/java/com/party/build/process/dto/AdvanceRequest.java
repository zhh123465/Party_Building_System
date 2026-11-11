package com.party.build.process.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AdvanceRequest {
    @NotBlank
    private String targetStage;
    @NotNull
    private LocalDate meetingDate;
    private LocalDate filingDate;
    private LocalDate approvalDate;
    @NotBlank
    private String reason;
    private boolean confirm;
}