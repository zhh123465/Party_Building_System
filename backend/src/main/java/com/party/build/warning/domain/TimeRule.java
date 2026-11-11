package com.party.build.warning.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("time_rule")
public class TimeRule {
    @TableId(type = IdType.AUTO)
    private Long ruleId;
    private String ruleCode;
    private Long academicYearId;
    private String nodeCode;
    private String stage;
    private String nodeName;
    private String startEvent;
    private String nodeType;
    private String dependsOnNode;
    private Integer offsetValue;
    private String offsetUnit;
    private String warnOffsets;
    private String responsibleRole;
    private Integer enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}