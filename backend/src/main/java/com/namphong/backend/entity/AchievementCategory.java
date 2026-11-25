package com.namphong.backend.entity;

/**
 * How an achievement's {@code threshold} is checked (see AchievementService).
 * New achievements of an existing category need only a row in {@code achievements}.
 */
public enum AchievementCategory {
    /** completed sessions >= threshold */
    SESSIONS,
    /** total completed seconds >= threshold */
    TOTAL_TIME,
    /** the session just finished lasted >= threshold seconds */
    SESSION_LENGTH,
    /** the session just finished started between 04:00 and threshold:00 (user zone) */
    EARLY_BIRD,
    /** the session just finished started between threshold:00 and 04:00 (user zone) */
    NIGHT_OWL,
    /** consecutive study days (current streak) >= threshold */
    STREAK,
    /** today's time reached the active overall daily goal */
    DAILY_GOAL,
    /** completed tasks >= threshold */
    TASKS,
    /** groups joined (or created) >= threshold */
    GROUPS,
    /** reached the target of a running group challenge */
    CHALLENGE
}
