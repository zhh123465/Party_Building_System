package com.party.build.process.guard;

import com.party.build.auth.security.UserPrincipal;
import com.party.build.person.domain.Person;
import com.party.build.process.dto.AdvanceRequest;
import com.party.build.process.dto.MaterialSummary;
import com.party.build.warning.domain.Warning;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class GuardContext {
    private Person person;
    private String targetStage;
    private AdvanceRequest request;
    private MaterialSummary materialSummary;
    private List<Warning> warnings;
    private UserPrincipal principal;
}