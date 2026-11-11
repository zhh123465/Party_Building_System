package com.party.build.archive.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PersonArchiveVO {
    private Long personId;
    private String name;
    private String studentNo;
    private String currentStage;
    private List<ArchiveStageVO> stages;
    private List<ArchiveRecordVO> archives;
}