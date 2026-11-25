package com.namphong.backend.dto;

import java.util.List;

public record NotificationPageResponse(List<NotificationResponse> items, int page, int size, long totalElements, boolean hasNext, long unread) {}
