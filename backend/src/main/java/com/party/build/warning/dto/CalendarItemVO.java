package com.party.build.warning.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalendarItemVO {
    private Long warningId;
    private String date;
    private String personName;
    private String nodeName;
    private String level;
    private String status;
}