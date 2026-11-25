package com.namphong.backend.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StreakCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 25);

    private static StreakCalculator.Streak streak(LocalDate... days) {
        return StreakCalculator.compute(List.of(days), TODAY);
    }

    @Test
    void noStudyDays() {
        assertEquals(StreakCalculator.Streak.NONE, StreakCalculator.compute(List.of(), TODAY));
        assertEquals(StreakCalculator.Streak.NONE, StreakCalculator.compute(null, TODAY));
    }

    @Test
    void consecutiveDaysEndingToday() {
        assertEquals(new StreakCalculator.Streak(3, 3), streak(TODAY, TODAY.minusDays(1), TODAY.minusDays(2)));
    }

    @Test
    void streakEndingYesterdayIsStillAlive() {
        assertEquals(new StreakCalculator.Streak(2, 2), streak(TODAY.minusDays(1), TODAY.minusDays(2)));
    }

    @Test
    void aMissedDayBreaksTheCurrentStreakButNotTheLongest() {
        StreakCalculator.Streak s = streak(TODAY.minusDays(2), TODAY.minusDays(3), TODAY.minusDays(4), TODAY.minusDays(5));
        assertEquals(0, s.current());
        assertEquals(4, s.longest());
    }

    @Test
    void gapsDuplicatesAndOrderDoNotMatter() {
        StreakCalculator.Streak s = StreakCalculator.compute(List.of(
                TODAY, TODAY.minusDays(10), TODAY, TODAY.minusDays(1), TODAY.minusDays(11),
                TODAY.minusDays(12), TODAY.minusDays(13)), TODAY);
        assertEquals(2, s.current());
        assertEquals(4, s.longest());
    }

    @Test
    void futureDaysAreIgnored() {
        assertEquals(new StreakCalculator.Streak(1, 1), streak(TODAY.plusDays(1), TODAY));
    }
}
