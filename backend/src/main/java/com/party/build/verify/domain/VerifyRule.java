package com.party.build.verify.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("verify_rule")
public class VerifyRule {
    @TableId(type = IdType.AUTO)
    private Long ruleId;
    private String ruleCode;
    private String ruleName;
    private String stage;
    private Long materialTypeId;
    private String ruleType;
    private String params;
    private String severity;
    private Integer enabled;
    private Integer versionNo;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}