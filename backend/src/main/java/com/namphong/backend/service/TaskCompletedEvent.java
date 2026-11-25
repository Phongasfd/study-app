package com.namphong.backend.service;

import java.util.UUID;

/** A task moved to DONE. Handled after commit (achievements). */
public record TaskCompletedEvent(UUID userId, UUID taskId) {
}
