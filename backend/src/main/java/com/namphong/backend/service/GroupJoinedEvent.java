package com.namphong.backend.service;

import java.util.UUID;

/** A user became a member of a group (created it, joined, was approved or added). Handled after commit. */
public record GroupJoinedEvent(UUID userId, UUID groupId) {
}
