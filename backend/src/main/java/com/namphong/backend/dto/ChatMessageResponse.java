package com.namphong.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class ChatMessageResponse {
    private String id;
    private String groupId;
    private String senderId;
    private String senderName;
    private String content;
    private LocalDateTime timestamp;
}
