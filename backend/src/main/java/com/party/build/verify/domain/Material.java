package com.party.build.verify.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("material")
public class Material {
    @TableId(type = IdType.AUTO)
    private Long materialId;
    private Long personId;
    private String stage;
    private Long materialTypeId;
    private Long checklistItemId;
    private Integer required;
    private String status;
    private Long currentVersionId;
    private LocalDate dueDate;
    private Long reviewerId;
    private LocalDateTime lastReviewAt;
    private Integer archived;
    @Version
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}