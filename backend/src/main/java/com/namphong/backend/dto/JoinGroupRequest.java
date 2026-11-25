package com.namphong.backend.dto;

import jakarta.validation.constraints.Size;

/** Optional note to the owner of a private group. */
public record JoinGroupRequest(@Size(max = 500) String message) {}
