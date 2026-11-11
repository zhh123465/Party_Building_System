package com.party.build.process.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RollbackRequest {
    @NotBlank
    private String targetStage;
    @NotBlank
    private String reason;
    private boolean confirm;
}