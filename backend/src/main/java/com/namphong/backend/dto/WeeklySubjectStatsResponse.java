package com.namphong.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class WeeklySubjectStatsResponse {
    private UUID subjectId;
    private String subjectName;
    private Long totalDurationSeconds;
}
