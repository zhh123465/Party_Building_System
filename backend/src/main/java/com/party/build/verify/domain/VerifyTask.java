package com.party.build.verify.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("verify_task")
public class VerifyTask {
    @TableId(type = IdType.AUTO)
    private Long taskId;
    private Long materialId;
    private Long versionId;
    private String status;
    private Integer attemptNo;
    private String modelProvider;
    private String modelName;
    private String promptVersion;
    private String errorCode;
    private String errorMsg;
    private LocalDateTime queuedAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Integer durationMs;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}