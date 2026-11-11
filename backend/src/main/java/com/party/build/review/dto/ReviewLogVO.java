package com.party.build.review.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewLogVO {
    private Long reviewId;
    private Long materialId;
    private Long versionId;
    private Long reportId;
    private Long reviewerId;
    private String reviewerName;
    private String reviewLevel;
    private String conclusion;
    private String reasonCode;
    private String opinion;
    private LocalDateTime createdAt;
}