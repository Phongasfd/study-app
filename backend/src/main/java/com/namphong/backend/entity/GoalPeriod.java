package com.namphong.backend.entity;

import java.time.DayOfWeek;
import java.time.LocalDate;

/** The window a study goal is measured over. Weeks start on Monday, like the stats page. */
public enum GoalPeriod {
    DAILY(24 * 60),
    WEEKLY(7 * 24 * 60),
    MONTHLY(31 * 24 * 60);

    /** A target can't exceed the length of its period. */
    private final int maxMinutes;

    GoalPeriod(int maxMinutes) {
        this.maxMinutes = maxMinutes;
    }

    public int maxMinutes() {
        return maxMinutes;
    }

    /** First day of the period containing {@code today} (inclusive). */
    public LocalDate start(LocalDate today) {
        return switch (this) {
            case DAILY -> today;
            case WEEKLY -> today.with(DayOfWeek.MONDAY);
            case MONTHLY -> today.withDayOfMonth(1);
        };
    }

    /** First day after the period containing {@code today} (exclusive). */
    public LocalDate endExclusive(LocalDate today) {
        return switch (this) {
            case DAILY -> today.plusDays(1);
            case WEEKLY -> today.with(DayOfWeek.MONDAY).plusWeeks(1);
            case MONTHLY -> today.withDayOfMonth(1).plusMonths(1);
        };
    }
}
