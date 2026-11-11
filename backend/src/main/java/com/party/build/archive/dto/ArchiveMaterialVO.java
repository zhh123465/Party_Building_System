package com.party.build.archive.dto;

import lombok.Data;

import java.util.List;

@Data
public class ArchiveMaterialVO {
    private Long materialId;
    private Long materialTypeId;
    private String materialTypeName;
    private String required;
    private String status;
    private Integer versionCount;
    private Integer latestVersionNo;
    private String latestFileName;
    private Boolean archived;
}