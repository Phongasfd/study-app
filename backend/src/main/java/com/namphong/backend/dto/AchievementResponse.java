package com.namphong.backend.dto;

import com.namphong.backend.entity.AchievementCategory;
import java.time.LocalDateTime;

public record AchievementResponse(String code, String name, String description, String icon, AchievementCategory category, int threshold, boolean unlocked, LocalDateTime unlockedAt) {}
