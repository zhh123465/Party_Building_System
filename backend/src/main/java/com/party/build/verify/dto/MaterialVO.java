package com.party.build.verify.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MaterialVO {
    private Long materialId;
    private Long personId;
    private String stage;
    private Long materialTypeId;
    private String materialTypeName;
    private String materialTypeCode;
    private Integer required;
    private String status;
    private Long currentVersionId;
    private Integer latestVersionNo;
    private String latestFileName;
    private LocalDateTime latestUploadedAt;
    private String taskStatus;
    private Long reportId;
    private String overallConclusion;
    private List<String> verifyPoints;
}