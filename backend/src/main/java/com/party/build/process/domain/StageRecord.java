package com.party.build.process.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("stage_record")
public class StageRecord {
    @TableId(type = IdType.AUTO)
    private Long recordId;
    private Long personId;
    private String fromStage;
    private String toStage;
    private String changeType;
    private LocalDateTime changeDate;
    private Long operatorId;
    private String reason;
    private String evidence;
    private Long templateId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}