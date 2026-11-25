package com.namphong.backend.dto;

import com.namphong.backend.entity.GroupChallenge;
import java.time.LocalDate;
import java.util.UUID;

public record ChallengeResponse(UUID id, UUID groupId, String title, String description, int targetSeconds, LocalDate startDate, LocalDate endDate, GroupChallenge.Status status, String createdByName, long myProgressSeconds, int myPercent) {}
