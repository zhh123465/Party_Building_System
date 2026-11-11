package com.party.build.warning.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class WarningVO {
    private Long warningId;
    private Long personId;
    private String personName;
    private String studentNo;
    private Long branchId;
    private String stage;
    private String nodeCode;
    private String nodeName;
    private String cycleKey;
    private LocalDate dueDate;
    private Long daysLeft;
    private String level;
    private String status;
    private String handledByName;
    private LocalDateTime handledAt;
    private String handleNote;
}
