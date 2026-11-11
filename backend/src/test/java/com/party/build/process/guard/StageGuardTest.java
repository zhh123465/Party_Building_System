package com.party.build.process.guard;

import com.party.build.auth.security.UserPrincipal;
import com.party.build.person.domain.Person;
import com.party.build.process.dto.AdvanceRequest;
import com.party.build.process.dto.MaterialSummary;
import com.party.build.warning.domain.Warning;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StageGuardTest {

    private GuardContext context(AdvanceRequest request, MaterialSummary summary, List<Warning> warnings) {
        Person person = new Person();
        person.setPersonId(1L);
        person.setName("张明");
        person.setCurrentStage("S0");
        UserPrincipal principal = new UserPrincipal(1L, "admin", "管理员", "R1", 1L, null, 0, true);
        return new GuardContext(person, "S1", request, summary, warnings, principal);
    }

    @Test
    void materialGuardShouldListAllIssues() {
        MaterialSummary summary = new MaterialSummary();
        summary.setRequiredCount(2);
        summary.setIssues(List.of("缺少材料：入党申请书", "材料未通过：思想汇报（当前 M5）"));
        GuardResult result = new MaterialGuard().check(context(null, summary, List.of()));
        assertThat(result.getBlockers()).hasSize(2);
    }

    @Test
    void timeGuardShouldBlockOverdueAndWarnNear() {
        Warning overdue = new Warning();
        overdue.setLevel("OVERDUE");
        overdue.setStatus("OPEN");
        overdue.setNodeCode("T-01");
        overdue.setDueDate(LocalDate.of(2026, 10, 1));
        Warning near = new Warning();
        near.setLevel("NEAR");
        near.setStatus("OPEN");
        near.setNodeCode("T-03");
        near.setDueDate(LocalDate.of(2026, 11, 20));

        GuardResult result = new TimeGuard().check(context(null, new MaterialSummary(), List.of(overdue, near)));
        assertThat(result.getBlockers()).anyMatch(b -> "TIME".equals(b.getCode()));
        assertThat(result.getWarnings()).anyMatch(w -> w.contains("T-03"));
    }

    @Test
    void confirmGuardShouldRequireMeetingDateReasonAndConfirm() {
        GuardResult empty = new ConfirmGuard().check(context(null, new MaterialSummary(), List.of()));
        assertThat(empty.getRequiredFields()).contains("meetingDate", "reason", "confirm");

        AdvanceRequest request = new AdvanceRequest();
        request.setTargetStage("S1");
        request.setMeetingDate(LocalDate.of(2026, 11, 11));
        request.setReason("支部委员会讨论确定");
        request.setConfirm(false);
        GuardResult result = new ConfirmGuard().check(context(request, new MaterialSummary(), List.of()));
        assertThat(result.getBlockers()).anyMatch(b -> "CONFIRM".equals(b.getCode()));
    }

    @Test
    void filingGuardShouldRequireFilingDateForS1() {
        GuardResult result = new FilingGuard().check(context(null, new MaterialSummary(), List.of()));
        assertThat(result.getRequiredFields()).contains("filingDate");

        AdvanceRequest request = new AdvanceRequest();
        request.setTargetStage("S1");
        GuardResult blocked = new FilingGuard().check(context(request, new MaterialSummary(), List.of()));
        assertThat(blocked.getBlockers()).anyMatch(b -> "FILING".equals(b.getCode()));
    }
}