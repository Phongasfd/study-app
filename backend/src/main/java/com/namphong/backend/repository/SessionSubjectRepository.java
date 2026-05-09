package com.namphong.backend.repository;

import com.namphong.backend.entity.SessionSubject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SessionSubjectRepository extends JpaRepository<SessionSubject, UUID> {
    List<SessionSubject> findBySessionId(UUID sessionId);
    List<SessionSubject> findBySessionIdIn(List<UUID> sessionIds);
}
