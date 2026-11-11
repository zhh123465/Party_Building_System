package com.party.build.person.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class PersonImportRow {
    @ExcelProperty("姓名")
    private String name;
    @ExcelProperty("学号")
    private String studentNo;
    @ExcelProperty("性别")
    private String gender;
    @ExcelProperty("学院")
    private String college;
    @ExcelProperty("专业")
    private String major;
    @ExcelProperty("班级")
    private String className;
    @ExcelProperty("所属支部")
    private String branchName;
    @ExcelProperty("联系电话")
    private String phone;
    @ExcelProperty("身份证号")
    private String idCard;
    @ExcelProperty("递交申请书日期")
    private String applicationDate;
    @ExcelProperty("当前阶段")
    private String currentStage;
    @ExcelProperty("阶段起始日期")
    private String stageStartDate;
    @ExcelProperty("备注")
    private String remark;
}