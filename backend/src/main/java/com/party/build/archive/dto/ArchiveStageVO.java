package com.party.build.archive.dto;

import lombok.Data;

import java.util.List;

@Data
public class ArchiveStageVO {
    private String stage;
    private int requiredCount;
    private int passedCount;
    private boolean complete;
    private List<ArchiveMaterialVO> materials;
}