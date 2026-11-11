package com.party.build.person.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("person")
public class Person {
    @TableId(type = IdType.AUTO)
    private Long personId;
    private String studentNo;
    private String name;
    private String gender;
    private String college;
    private String major;
    private String className;
    private Long branchId;
    private byte[] phoneCipher;
    private String phoneMask;
    private byte[] idCardCipher;
    private String idCardMask;
    private LocalDate applicationDate;
    private String currentStage;
    private LocalDate stageStartDate;
    private Long templateId;
    private String status;
    private Long sourceBatchId;
    private String remark;
    @Version
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}