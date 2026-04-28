package com.namphong.backend.service;

import com.namphong.backend.entity.SessionSubject;
import com.namphong.backend.entity.StudySession;
import com.namphong.backend.entity.Subject;
import com.namphong.backend.repository.SessionSubjectRepository;
import com.namphong.backend.repository.StudySessionRepository;
import com.namphong.backend.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionSubjectService {
    private final SessionSubjectRepository sessionSubjectRepository;
    private final StudySessionRepository studySessionRepository;
    private final SubjectRepository subjectRepository;

    public SessionSubject addSubjectToSession(UUID sessionId, UUID subjectId) {
        StudySession session = studySessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new RuntimeException("Subject not found"));

        SessionSubject sessionSubject = new SessionSubject();
        sessionSubject.setSession(session);
        sessionSubject.setSubject(subject);

        return sessionSubjectRepository.save(sessionSubject);
    }

    public List<SessionSubject> getSubjectsBySession(UUID sessionId) {
        return sessionSubjectRepository.findBySessionId(sessionId);
    }

}
