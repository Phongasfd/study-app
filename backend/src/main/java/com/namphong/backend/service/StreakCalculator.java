package com.namphong.backend.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.TreeSet;

/** Consecutive study days. Pure function, no database access. */
public final class StreakCalculator {

    public record Streak(int current, int longest) {
        public static final Streak NONE = new Streak(0, 0);
    }

    private StreakCalculator() {
    }

    /**
     * @param studyDays days with credited study time, in any order (duplicates allowed)
     * @param today     the current day
     * @return the current streak (alive if the latest study day is today or yesterday, so it
     *         isn't shown as broken before the user had a chance to study today) and the longest
     */
    public static Streak compute(Collection<LocalDate> studyDays, LocalDate today) {
        if (studyDays == null || studyDays.isEmpty()) {
            return Streak.NONE;
        }
        NavigableSet<LocalDate> days = new TreeSet<>(studyDays);
        // days in the future (clock skew) can't extend a streak
        days = days.headSet(today, true);
        if (days.isEmpty()) {
            return Streak.NONE;
        }

        int longest = 0;
        int run = 0;
        LocalDate previous = null;
        for (LocalDate day : days) {
            run = previous != null && previous.plusDays(1).equals(day) ? run + 1 : 1;
            longest = Math.max(longest, run);
            previous = day;
        }

        int current = 0;
        LocalDate latest = days.last();
        if (!latest.isBefore(today.minusDays(1))) {
            LocalDate expected = latest;
            Iterator<LocalDate> newestFirst = days.descendingIterator();
            while (newestFirst.hasNext() && newestFirst.next().equals(expected)) {
                current++;
                expected = expected.minusDays(1);
            }
        }
        return new Streak(current, longest);
    }
}
