package com.namphong.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class WeeklyAggregateResponse {
    private String weekLabel;
    private LocalDate weekStart;
    private LocalDate weekEnd;
    private Integer totalDuration;
}
