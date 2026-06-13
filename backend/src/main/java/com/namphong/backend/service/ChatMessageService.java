package com.namphong.backend.service;

import com.namphong.backend.dto.ChatMessageRequest;
import com.namphong.backend.dto.ChatMessageResponse;
import com.namphong.backend.entity.ChatMessage;
import com.namphong.backend.entity.UserEntity;
import com.namphong.backend.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatMessageService {
    
    private final ChatMessageRepository chatMessageRepository;

    public ChatMessageResponse createMessage(ChatMessageRequest request, UserEntity currentUser) {
        ChatMessage message = ChatMessage.builder()
                .groupId(request.getGroupId())
                .senderId(currentUser.getId().toString())
                .senderName(currentUser.getUsername())
                .content(request.getContent())
                .timestamp(LocalDateTime.now())
                .build();
                
        message = chatMessageRepository.save(message);
        
        return toResponse(message);
    }
    
    public List<ChatMessageResponse> getMessagesByGroupId(String groupId) {
        List<ChatMessage> messages = chatMessageRepository.findByGroupIdOrderByTimestampAsc(groupId);
        return messages.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    private ChatMessageResponse toResponse(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .groupId(message.getGroupId())
                .senderId(message.getSenderId())
                .senderName(message.getSenderName())
                .content(message.getContent())
                .timestamp(message.getTimestamp())
                .build();
    }
}
