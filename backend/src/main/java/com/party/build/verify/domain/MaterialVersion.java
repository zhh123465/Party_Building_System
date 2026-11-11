package com.party.build.verify.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("material_version")
public class MaterialVersion {
    @TableId(type = IdType.AUTO)
    private Long versionId;
    private Long materialId;
    private Integer versionNo;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String mimeType;
    private String sha256;
    private Integer pageCount;
    private String textContent;
    private String textStatus;
    private Integer ocrUsed;
    private Long uploadedBy;
    private LocalDateTime uploadedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}