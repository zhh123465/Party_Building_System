package com.party.build.checklist.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("material_type")
public class MaterialType {
    @TableId(type = IdType.AUTO)
    private Long typeId;
    private String typeCode;
    private String typeName;
    private String stageScope;
    private String fileFormats;
    private Integer defaultRequired;
    private Integer sortNo;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}