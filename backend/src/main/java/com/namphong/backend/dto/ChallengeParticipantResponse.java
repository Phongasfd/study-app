package com.namphong.backend.dto;

import java.util.UUID;

public record ChallengeParticipantResponse(UUID userId, String username, long progressSeconds, int percent, boolean completed, int rank) {}
