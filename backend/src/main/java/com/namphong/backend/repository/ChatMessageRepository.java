package com.namphong.backend.repository;

import com.namphong.backend.entity.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    List<ChatMessage> findByGroupIdOrderByTimestampAsc(String groupId);
}
