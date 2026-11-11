package com.party.build.checklist.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateValidateResponse {
    private boolean valid;
    private List<String> errors = new ArrayList<>();
}