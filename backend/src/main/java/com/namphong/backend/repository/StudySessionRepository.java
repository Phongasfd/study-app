package com.namphong.backend.repository;

import com.namphong.backend.entity.StudySession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StudySessionRepository extends JpaRepository<StudySession, UUID> {
}
