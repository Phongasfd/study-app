package com.namphong.backend.dto;

import com.namphong.backend.entity.GoalPeriod;
import java.time.LocalDate;
import java.util.UUID;

/** periodEnd is inclusive (last day of the period). */
public record GoalProgressResponse(UUID id, GoalPeriod period, UUID subjectId, String subjectName, int targetSeconds, long progressSeconds, int percent, boolean achieved, LocalDate periodStart, LocalDate periodEnd) {}
