package com.namphong.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
@Builder
public class ChatMessageResponse {
    private String id;
    private UUID groupId;
    private UUID senderId;
    private String senderName;
    private String content;
    private LocalDateTime timestamp;
}
