package com.namphong.backend.dto;

public record TaskSummaryResponse(long todo, long inProgress, long done, long overdue, long dueToday) {}
