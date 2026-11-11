package com.party.build.archive.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ArchiveRecordVO {
    private Long archiveId;
    private Long personId;
    private String stage;
    private String archiveNo;
    private String status;
    private Long archivedBy;
    private String archivedByName;
    private LocalDateTime archivedAt;
    private Long fileSize;
}
