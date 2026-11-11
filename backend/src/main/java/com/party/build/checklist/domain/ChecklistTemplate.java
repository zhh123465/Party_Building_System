package com.party.build.checklist.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("checklist_template")
public class ChecklistTemplate {
    @TableId(type = IdType.AUTO)
    private Long templateId;
    private String templateCode;
    private String templateName;
    private Long academicYearId;
    private Integer versionNo;
    private String status;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}