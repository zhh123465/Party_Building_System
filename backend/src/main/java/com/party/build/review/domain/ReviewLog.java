package com.party.build.review.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("review_log")
public class ReviewLog {
    @TableId(type = IdType.AUTO)
    private Long reviewId;
    private Long materialId;
    private Long versionId;
    private Long reportId;
    private Long reviewerId;
    private String reviewLevel;
    private String conclusion;
    private String reasonCode;
    private String opinion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}