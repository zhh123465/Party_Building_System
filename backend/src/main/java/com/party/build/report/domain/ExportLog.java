package com.party.build.report.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("export_log")
public class ExportLog {
    @TableId(type = IdType.AUTO)
    private Long exportId;
    private Long userId;
    private String exportType;
    private String filterJson;
    private String fileName;
    private String filePath;
    private String watermarkText;
    private Integer rowCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}