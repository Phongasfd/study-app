package com.namphong.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GoalUpdateRequest(@NotNull @Min(1) Integer targetMinutes) {}
