package com.party.build.warning.service;

import com.party.build.warning.domain.TimeRule;
import com.party.build.warning.dto.ScheduleResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleCalculatorTest {
    private TimeRule rule(String code, String name, int offset, String unit, String depends) {
        TimeRule r = new TimeRule();
        r.setNodeCode(code);
        r.setNodeName(name);
        r.setOffsetValue(offset);
        r.setOffsetUnit(unit);
        r.setDependsOnNode(depends);
        return r;
    }

    private List<TimeRule> rules() {
        List<TimeRule> list = new ArrayList<>();
        list.add(rule("T-01", "首次谈话", 30, "DAY", null));
        list.add(rule("T-02", "确定积极分子", 60, "DAY", "T-01"));
        list.add(rule("T-04", "培养考察期满", 12, "MONTH", "T-02"));
        list.add(rule("T-05", "政治审查完成", 60, "DAY", "T-04"));
        list.add(rule("T-06", "集中培训完成", 0, "DAY", "T-04"));
        list.add(rule("T-07", "支部大会讨论接收", 30, "DAY", "T-05,T-06"));
        list.add(rule("T-08", "上级审批", 90, "DAY", "T-07"));
        list.add(rule("T-09", "预备期起算", 0, "DAY", "T-08"));
        list.add(rule("T-10", "预备期满", 12, "MONTH", "T-09"));
        list.add(rule("T-11", "转正审批", 90, "DAY", "T-10"));
        return list;
    }

    @Test
    void shouldReverseFromTargetDate() {
        LocalDate today = LocalDate.of(2026, 11, 11);
        LocalDate target = LocalDate.of(2030, 6, 30);
        ScheduleResponse response = ScheduleCalculator.calculate(rules(), "T-11", target, today);

        assertThat(response.isFeasible()).isTrue();
        assertThat(response.getNodes()).anyMatch(n -> "T-11".equals(n.getRuleCode()) && target.equals(n.getLatestDue()));
        assertThat(response.getNodes()).anyMatch(n -> "T-10".equals(n.getRuleCode())
                && LocalDate.of(2030, 4, 1).equals(n.getLatestDue()));
        assertThat(response.getNodes()).anyMatch(n -> "T-01".equals(n.getRuleCode()));
    }

    @Test
    void shouldMarkInfeasibleWhenTargetTooClose() {
        LocalDate today = LocalDate.of(2026, 11, 11);
        LocalDate target = LocalDate.of(2027, 6, 30);
        ScheduleResponse response = ScheduleCalculator.calculate(rules(), "T-11", target, today);
        assertThat(response.isFeasible()).isFalse();
    }
}