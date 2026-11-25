package com.namphong.backend.dto;

import java.util.UUID;

public record ProfileSummaryResponse(UUID userId, String username, long totalSeconds, long completedSessions, int currentStreak, int longestStreak, long achievementsUnlocked, long achievementsTotal, long tasksDone, long groupsJoined) {}
