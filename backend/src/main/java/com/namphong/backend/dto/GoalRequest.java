package com.namphong.backend.dto;

import com.namphong.backend.entity.GoalPeriod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** subjectId null: the goal counts every session. */
public record GoalRequest(UUID subjectId, @NotNull GoalPeriod period, @NotNull @Min(1) Integer targetMinutes) {}
