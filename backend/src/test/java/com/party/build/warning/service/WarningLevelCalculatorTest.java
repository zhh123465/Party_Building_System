package com.party.build.warning.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class WarningLevelCalculatorTest {
    private final LocalDate today = LocalDate.of(2026, 11, 11);

    @Test
    void shouldMapLevels() {
        assertThat(WarningLevelCalculator.level(today.minusDays(1), today, "[30,15,7,3]")).isEqualTo("OVERDUE");
        assertThat(WarningLevelCalculator.level(today, today, "[30,15,7,3]")).isEqualTo("URGENT");
        assertThat(WarningLevelCalculator.level(today.plusDays(2), today, "[30,15,7,3]")).isEqualTo("URGENT");
        assertThat(WarningLevelCalculator.level(today.plusDays(10), today, "[30,15,7,3]")).isEqualTo("NEAR");
        assertThat(WarningLevelCalculator.level(today.plusDays(20), today, "[30,15,7,3]")).isEqualTo("INFO");
        assertThat(WarningLevelCalculator.level(today.plusDays(40), today, "[30,15,7,3]")).isEqualTo("NONE");
    }
}