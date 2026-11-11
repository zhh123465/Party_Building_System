package com.party.build.person.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PersonCreateRequest {
    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9]{4,32}$", message = "学号格式不正确")
    private String studentNo;
    @NotBlank
    @Size(min = 2, max = 50)
    private String name;
    private String gender;
    private String college;
    private String major;
    private String className;
    @NotNull
    private Long branchId;
    private String phone;
    private String idCard;
    @NotNull
    private LocalDate applicationDate;
    private String currentStage = "S0";
    @NotNull
    private LocalDate stageStartDate;
    private String remark;
}