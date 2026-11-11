package com.party.build.verify.dto;

import lombok.Data;

import java.util.List;

@Data
public class MaterialDetailVO {
    private MaterialVO material;
    private List<MaterialVersionVO> versions;
    private VerifyReportVO latestReport;
}