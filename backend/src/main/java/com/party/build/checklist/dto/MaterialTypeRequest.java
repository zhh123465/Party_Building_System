package com.party.build.checklist.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MaterialTypeRequest {
    @NotBlank
    private String typeCode;
    @NotBlank
    private String typeName;
    @NotBlank
    private String stageScope;
    private String fileFormats = "pdf,jpg,png,docx";
    private Integer defaultRequired = 1;
    private Integer sortNo = 0;
    private Integer status = 1;
}