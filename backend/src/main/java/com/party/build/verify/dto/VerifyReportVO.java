package com.party.build.verify.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class VerifyReportVO {
    private Long reportId;
    private Long materialId;
    private Long versionId;
    private Long taskId;
    private String ruleConclusion;
    private String semanticConclusion;
    private String overallConclusion;
    private BigDecimal confidence;
    private String modelName;
    private String promptVersion;
    private Integer durationMs;
    private LocalDateTime createdAt;
    private List<FindingVO> findings;

    @Data
    public static class FindingVO {
        private Long findingId;
        private String layer;
        private String ruleCode;
        private String pointName;
        private String conclusion;
        private String severity;
        private String evidenceText;
        private Integer pageNo;
        private Integer charStart;
        private Integer charEnd;
        private String suggestion;
        private BigDecimal confidence;
    }
}