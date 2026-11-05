package com.namphong.backend.service;

import java.util.UUID;

/**
 * A user's study session started or finished, so every group they belong to has a new ranking.
 * Published inside the transaction; {@code RankingBroadcaster} reacts only after it commits.
 */
public record StudySessionChangedEvent(UUID userId) {
}
