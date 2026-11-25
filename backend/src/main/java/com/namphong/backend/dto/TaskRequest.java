package com.namphong.backend.dto;

import com.namphong.backend.entity.TaskPriority;
import com.namphong.backend.entity.TaskStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

/** priority defaults to MEDIUM and status to TODO when omitted. */
public record TaskRequest(@NotBlank @Size(max = 200) String title, @Size(max = 2000) String notes, UUID subjectId, TaskPriority priority, TaskStatus status, LocalDate dueDate, @Min(1) @Max(10000) Integer estimatedMinutes) {}
