package com.namphong.backend.dto;

import com.namphong.backend.entity.NotificationType;
import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(UUID id, NotificationType type, String title, String message, String link, boolean read, LocalDateTime createdAt) {}
