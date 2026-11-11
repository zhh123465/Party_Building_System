package com.party.build.checklist.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("checklist_item")
public class ChecklistItem {
    @TableId(type = IdType.AUTO)
    private Long itemId;
    private Long templateId;
    private String stage;
    private Long materialTypeId;
    private Integer required;
    private Integer minCount;
    private Integer maxCount;
    private Integer minPages;
    private String verifyPoints;
    private Integer sortNo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}