package com.namphong.backend.dto;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class UserResponseDTO {
    private final UUID id;
    private final String email;
    private final String username;
    private final LocalDateTime createdAt;

    public UserResponseDTO(UUID id, String email, String username, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.createdAt = createdAt;
    }
}
// for registering and getMe