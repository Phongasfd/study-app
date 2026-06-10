package com.namphong.backend.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "chat_messages") // use mongodb for chat message 
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    private String id;
    
    private UUID groupId;
    
    private UUID senderId;
    
    private String senderName;
    
    private String content;
    
    private LocalDateTime timestamp;
}
