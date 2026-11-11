package com.party.build.verify.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("verify_finding")
public class VerifyFinding {
    @TableId(type = IdType.AUTO)
    private Long findingId;
    private Long reportId;
    private String layer;
    private String ruleCode;
    private String pointName;
    private String conclusion;
    private String severity;
    private String evidenceText;
    private Integer pageNo;
    private Integer charStart;
    private Integer charEnd;
    private String suggestion;
    private BigDecimal confidence;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}