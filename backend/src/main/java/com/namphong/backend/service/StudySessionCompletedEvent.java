package com.namphong.backend.service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A session was completed and credited (duration > 0). Published inside the transaction;
 * {@code AchievementEvaluator} reacts only after it commits.
 */
public record StudySessionCompletedEvent(UUID userId, UUID sessionId, LocalDateTime startTime, int durationSeconds) {
}
