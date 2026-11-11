package com.party.build.warning.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("warning")
public class Warning {
    @TableId(type = IdType.AUTO)
    private Long warningId;
    private Long personId;
    private String stage;
    private Long timeRuleId;
    private String nodeCode;
    private String cycleKey;
    private LocalDate dueDate;
    private String level;
    private String status;
    private Long handledBy;
    private LocalDateTime handledAt;
    private String handleNote;
    private String lastNotifiedLevel;
    private Integer messageSent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}