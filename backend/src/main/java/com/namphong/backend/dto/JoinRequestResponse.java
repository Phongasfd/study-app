package com.namphong.backend.dto;

import com.namphong.backend.entity.JoinRequestStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record JoinRequestResponse(UUID id, UUID groupId, String groupName, UUID userId, String username, String message, JoinRequestStatus status, LocalDateTime createdAt) {}
