package com.namphong.backend.dto;

import com.namphong.backend.entity.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record TaskStatusRequest(@NotNull TaskStatus status) {}
