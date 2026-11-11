package com.party.build.process.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Blocker {
    private String code;
    private String message;
    private String action;
    private String detail;
}