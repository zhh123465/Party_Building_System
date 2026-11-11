package com.party.build.warning.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WarningHandleRequest {
    @NotBlank
    private String status;
    private String note;
}