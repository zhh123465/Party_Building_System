package com.party.build.review.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PendingReviewVO {
    private Long materialId;
    private Long personId;
    private String personName;
    private String studentNo;
    private String stage;
    private Long materialTypeId;
    private String materialTypeName;
    private String materialStatus;
    private Long versionId;
    private Integer versionNo;
    private String fileName;
    private Long reportId;
    private String overallConclusion;
    private String ruleConclusion;
    private String semanticConclusion;
    private Integer reportDurationMs;
    private LocalDateTime reportTime;
    private String taskStatus;
    private LocalDateTime latestUploadedAt;
    private Boolean initialReviewed;
}