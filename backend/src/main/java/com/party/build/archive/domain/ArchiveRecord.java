package com.party.build.archive.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("archive_record")
public class ArchiveRecord {
    @TableId(type = IdType.AUTO)
    private Long archiveId;
    private Long personId;
    private String stage;
    private String archiveNo;
    private String manifest;
    private String filePath;
    private String status;
    private Long archivedBy;
    private LocalDateTime archivedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}