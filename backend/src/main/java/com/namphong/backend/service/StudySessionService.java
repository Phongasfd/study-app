package com.namphong.backend.service;

import com.namphong.backend.dto.StudySessionRequest;
import com.namphong.backend.entity.SessionStatus;
import com.namphong.backend.entity.StudySession;
import com.namphong.backend.entity.UserEntity;
import com.namphong.backend.repository.StudySessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudySessionService {
    private final StudySessionRepository studySessionRepository;


    public StudySession createStudySession(StudySessionRequest studySession, UserEntity user) {
        StudySession session = new StudySession();
        session.setUser(user);
        session.setStartTime(studySession.getStartTime() != null ? studySession.getStartTime() : LocalDateTime.now());
        session.setStatus(SessionStatus.RUNNING);
        
        return studySessionRepository.save(session);
    }

    public StudySession completeStudySession(UUID sessionId, LocalDateTime endTime) {
        StudySession session = studySessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Study session not found"));
        LocalDateTime effectiveEndTime = endTime != null ? endTime : LocalDateTime.now();
        session.setEndTime(effectiveEndTime);
        session.setStatus(SessionStatus.COMPLETED);
        
        // Calculate duration if start time is present
        if (session.getStartTime() != null) {
            java.time.Duration duration = java.time.Duration.between(session.getStartTime(), effectiveEndTime);
            session.setDurationSeconds((int) duration.getSeconds());
        }

        return studySessionRepository.save(session);
    }
}
