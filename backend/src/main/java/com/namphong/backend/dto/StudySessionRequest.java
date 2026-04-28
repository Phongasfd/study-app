package com.namphong.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class StudySessionRequest {
    private LocalDateTime startTime;
}
