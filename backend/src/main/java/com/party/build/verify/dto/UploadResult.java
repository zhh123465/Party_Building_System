package com.party.build.verify.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UploadResult {
    private Long materialId;
    private Long versionId;
    private Integer versionNo;
    private Long taskId;
    private String materialStatus;
    private LocalDateTime uploadedAt;
}