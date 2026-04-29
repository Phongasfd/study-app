package com.namphong.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class DailyStatResponse {
    private UUID id;
    private UUID userId;
    private LocalDate date;
    private Integer totalDuration;
}
