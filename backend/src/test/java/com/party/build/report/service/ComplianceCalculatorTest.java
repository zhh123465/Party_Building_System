package com.party.build.report.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ComplianceCalculatorTest {

    @Test
    void shouldReturnPerfectScore() {
        ComplianceCalculator.Result r = ComplianceCalculator.calculate(5, 5, 0, 0, 0, 0, 0, 0, 0, 0, 10);
        assertThat(r.materialScore()).isEqualTo(100.0);
        assertThat(r.timeScore()).isEqualTo(100.0);
        assertThat(r.contentScore()).isEqualTo(100.0);
        assertThat(r.totalScore()).isEqualTo(100.0);
        assertThat(r.riskLevel()).isEqualTo("GREEN");
    }

    @Test
    void shouldPenalizeOverdueAndContent() {
        ComplianceCalculator.Result r = ComplianceCalculator.calculate(5, 3, 2, 1, 0, 0, 1, 2, 0, 0, 10);
        assertThat(r.materialScore()).isEqualTo(60.0);
        assertThat(r.timeScore()).isLessThan(50.0);
        assertThat(r.totalScore()).isLessThan(60.0);
        assertThat(r.riskLevel()).isEqualTo("RED");
    }

    @Test
    void shouldHandleNoMaterialsAndNoSemantics() {
        ComplianceCalculator.Result r = ComplianceCalculator.calculate(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        assertThat(r.totalScore()).isEqualTo(100.0);
    }
}