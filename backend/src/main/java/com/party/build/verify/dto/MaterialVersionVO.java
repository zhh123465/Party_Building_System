package com.party.build.verify.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MaterialVersionVO {
    private Long versionId;
    private Integer versionNo;
    private String fileName;
    private Long fileSize;
    private String mimeType;
    private String sha256;
    private Integer pageCount;
    private String textStatus;
    private Integer ocrUsed;
    private LocalDateTime uploadedAt;
}