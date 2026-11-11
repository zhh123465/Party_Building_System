package com.party.build.verify.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("verify_report")
public class VerifyReport {
    @TableId(type = IdType.AUTO)
    private Long reportId;
    private Long materialId;
    private Long versionId;
    private Long taskId;
    private String ruleConclusion;
    private String semanticConclusion;
    private String overallConclusion;
    private BigDecimal confidence;
    private String ruleVersion;
    private String modelName;
    private String promptVersion;
    private String evidence;
    private Integer durationMs;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}