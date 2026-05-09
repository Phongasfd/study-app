package com.namphong.backend.repository;

import com.namphong.backend.entity.SessionStatus;
import com.namphong.backend.entity.StudySession;
import com.namphong.backend.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface StudySessionRepository extends JpaRepository<StudySession, UUID> {
    List<StudySession> findByUserAndStatusAndStartTimeBetween(
            UserEntity user, SessionStatus status, LocalDateTime start, LocalDateTime end);

    List<StudySession> findByUserAndStartTimeBetween(
            UserEntity user, LocalDateTime start, LocalDateTime end);
}
