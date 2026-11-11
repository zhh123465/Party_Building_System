package com.party.build.warning.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ReverseScheduleRequest {
    @NotNull
    private Long personId;
    @NotNull
    private LocalDate targetDate;
    private String targetNode = "T-11";
}