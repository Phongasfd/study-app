package com.namphong.backend.dto;

import java.util.UUID;

/** status: JOINED (public group) or REQUESTED (private group, waiting for the owner). */
public record JoinResultResponse(String status, UUID groupId, UUID requestId) {
    public static final String JOINED = "JOINED";
    public static final String REQUESTED = "REQUESTED";
}
