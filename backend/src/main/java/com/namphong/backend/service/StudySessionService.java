package com.namphong.backend.service;

import com.namphong.backend.dto.StudySessionRequest;
import com.namphong.backend.entity.*;
import com.namphong.backend.repository.GroupMemberRepository;
import com.namphong.backend.repository.GroupRankingRepository;
import com.namphong.backend.repository.StudySessionRepository;
import com.namphong.backend.websocket.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudySessionService {
    private final StudySessionRepository studySessionRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupRankingRepository groupRankingRepository;
    private final ChatWebSocketHandler chatWebSocketHandler;

    @Transactional
    public StudySession createStudySession(StudySessionRequest studySession, UserEntity user) {

        // Check if user already has a running session
        Optional<StudySession> existingSession = studySessionRepository.findFirstByUserIdAndStatusOrderByStartTimeDesc(
                                user.getId(),
                                SessionStatus.RUNNING);

        if (existingSession.isPresent()) {

            StudySession oldSession = existingSession.get();
                            
            LocalDateTime now = LocalDateTime.now();
                            
            oldSession.setEndTime(now);
            oldSession.setStatus(SessionStatus.COMPLETED);
                            
            if (oldSession.getStartTime() != null) {
                oldSession.setDurationSeconds(
                    (int) Duration.between(
                    oldSession.getStartTime(),
                    now
                        ).getSeconds());
        }
                            
            studySessionRepository.save(oldSession);
    }

        StudySession session = new StudySession();
        session.setUser(user);
        session.setStartTime(studySession.getStartTime() != null ? studySession.getStartTime() : LocalDateTime.now());
        session.setStatus(SessionStatus.RUNNING);
        
        StudySession savedSession = studySessionRepository.save(session);

        // Broadcast ranking updates to all groups the user is in
        List<GroupMember> groupMembers = groupMemberRepository.findAllByUserId(user.getId());
        for (GroupMember member : groupMembers) {
            chatWebSocketHandler.broadcastRanking(member.getGroup().getId());
        }

        return savedSession;
    }

    @Transactional // to ensure the session is completed, if error => rollback the transaction
    public StudySession completeStudySession(UUID sessionId, LocalDateTime endTime) {
        
        StudySession session = studySessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Study session not found"));
        LocalDateTime effectiveEndTime = endTime != null ? endTime : LocalDateTime.now();
        session.setEndTime(effectiveEndTime);
        session.setStatus(SessionStatus.COMPLETED);
        
        // Calculate duration if start time is present
        if (session.getStartTime() != null) {
            Duration duration = Duration.between(session.getStartTime(), effectiveEndTime);
            session.setDurationSeconds((int) duration.getSeconds());
        }

        StudySession savedSession = studySessionRepository.save(session);

        UserEntity user = savedSession.getUser();
        List<GroupMember> groupMembers = groupMemberRepository.findAllByUserId(user.getId());

        // Update group rankings if duration is positive
        Integer duration = savedSession.getDurationSeconds();
        if (duration != null && duration > 0) {
            for (GroupMember member : groupMembers) {
                StudyGroup group = member.getGroup();
                
                GroupRanking ranking = groupRankingRepository.findByGroupIdAndUserId(group.getId(), user.getId())
                        .orElseGet(() -> GroupRanking.builder()
                                .group(group)
                                .user(user)
                                .totalDuration(0)
                                .build());
                                
                ranking.setTotalDuration((ranking.getTotalDuration() == null ? 0 : ranking.getTotalDuration()) + duration);
                groupRankingRepository.save(ranking);
            }
        }

        // Always broadcast live ranking updates to all groups the user belongs to when a session is completed
        for (GroupMember member : groupMembers) {
            chatWebSocketHandler.broadcastRanking(member.getGroup().getId());
        }

        return savedSession;
    }
}
