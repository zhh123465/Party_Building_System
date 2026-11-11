package com.party.build.process.guard;

import com.party.build.process.dto.Blocker;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class GuardResult {
    private List<Blocker> blockers = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private List<String> requiredFields = new ArrayList<>();
}