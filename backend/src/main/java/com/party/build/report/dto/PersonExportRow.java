package com.party.build.report.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class PersonExportRow {
    @ExcelProperty("姓名")
    private String name;
    @ExcelProperty("学号")
    private String studentNo;
    @ExcelProperty("支部")
    private String branchName;
    @ExcelProperty("阶段")
    private String stage;
    @ExcelProperty("阶段起始日")
    private String stageStartDate;
    @ExcelProperty("必填材料")
    private Integer required;
    @ExcelProperty("已通过")
    private Integer passed;
    @ExcelProperty("完成率")
    private String completion;
    @ExcelProperty("合规指数")
    private Double compliance;
    @ExcelProperty("风险等级")
    private String riskLevel;
    @ExcelProperty("逾期节点")
    private Integer overdue;
}