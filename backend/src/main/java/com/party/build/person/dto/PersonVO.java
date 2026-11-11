package com.party.build.person.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PersonVO {
    private Long personId;
    private String studentNo;
    private String name;
    private String gender;
    private String college;
    private String major;
    private String className;
    private Long branchId;
    private String phoneMask;
    private String idCardMask;
    private LocalDate applicationDate;
    private String currentStage;
    private LocalDate stageStartDate;
    private String status;
    private String remark;
}