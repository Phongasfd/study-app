package com.namphong.backend.dto;

import java.util.List;

public record ChallengeDetailResponse(ChallengeResponse challenge, List<ChallengeParticipantResponse> participants, int completedCount) {}
