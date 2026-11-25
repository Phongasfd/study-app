package com.namphong.backend.dto;

import com.namphong.backend.entity.TaskPriority;
import com.namphong.backend.entity.TaskStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record TaskResponse(UUID id, String title, String notes, UUID subjectId, String subjectName, TaskPriority priority, TaskStatus status, LocalDate dueDate, Integer estimatedMinutes, LocalDateTime completedAt, LocalDateTime createdAt, boolean overdue) {}
