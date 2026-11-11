package com.party.build.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewRequest {
    @NotBlank
    private String conclusion;
    private String reasonCode;
    private String opinion;
    @NotNull
    private Long versionId;
    private Long reportId;
}