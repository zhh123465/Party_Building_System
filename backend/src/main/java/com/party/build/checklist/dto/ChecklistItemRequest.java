package com.party.build.checklist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ChecklistItemRequest {
    @NotBlank
    private String stage;
    @NotNull
    private Long materialTypeId;
    private Integer required = 1;
    private Integer minCount = 1;
    private Integer maxCount;
    private Integer minPages;
    private List<String> verifyPoints;
    private Integer sortNo = 0;
}