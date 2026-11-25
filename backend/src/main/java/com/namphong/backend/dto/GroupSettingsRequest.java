package com.namphong.backend.dto;

import com.namphong.backend.entity.GroupVisibility;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** Fields left null keep their current value. */
public record GroupSettingsRequest(GroupVisibility visibility, @Size(max = 500) String description, @Min(1) @Max(500) Integer maxMembers) {}
