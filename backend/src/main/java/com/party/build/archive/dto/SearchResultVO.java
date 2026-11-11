package com.party.build.archive.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SearchResultVO {
    private Long materialId;
    private Long versionId;
    private Long personId;
    private String personName;
    private String studentNo;
    private String stage;
    private String materialTypeName;
    private String fileName;
    private String snippet;
    private LocalDateTime uploadedAt;
}