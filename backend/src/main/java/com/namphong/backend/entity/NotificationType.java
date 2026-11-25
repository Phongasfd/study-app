package com.namphong.backend.entity;

/**
 * The frontend translates a notification by its type; {@code message} carries the name it
 * refers to (group, achievement, challenge) and {@code title} is the English fallback.
 */
public enum NotificationType {
    ACHIEVEMENT_UNLOCKED,
    GROUP_MEMBER_ADDED,
    GROUP_MEMBER_REMOVED,
    JOIN_REQUEST_RECEIVED,
    JOIN_REQUEST_APPROVED,
    JOIN_REQUEST_REJECTED,
    CHALLENGE_CREATED
}
