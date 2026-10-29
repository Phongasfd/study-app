package com.namphong.backend.service;

import com.namphong.backend.entity.*;
import com.namphong.backend.exception.NotFoundException;
import com.namphong.backend.repository.GroupMemberRepository;
import com.namphong.backend.repository.GroupRankingRepository;
import com.namphong.backend.repository.StudySessionRepository;
import com.namphong.backend.websocket.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudySessionService {
    // A forgotten timer must not add days of study time to the ranking
    public static final Duration MAX_SESSION_DURATION = Duration.ofHours(12);

    private final StudySessionRepository studySessionRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupRankingRepository groupRankingRepository;
    private final ChatWebSocketHandler chatWebSocketHandler;
    private final Clock clock;

    // Start/end times always come from the server clock: client-sent times
    // let anyone backdate a session and inflate their ranking.
    @Transactional
    public StudySession createStudySession(UserEntity user) {
        LocalDateTime now = LocalDateTime.now(clock);

        // Check if user already has a running session -> close it (and credit its time) first
        Optional<StudySession> existingSession = studySessionRepository.findFirstByUserIdAndStatusOrderByStartTimeDesc(
                                user.getId(),
                                SessionStatus.RUNNING);
        existingSession.ifPresent(oldSession -> finishSession(oldSession, now));

        StudySession session = new StudySession();
        session.setUser(user);
        session.setStartTime(now);
        session.setStatus(SessionStatus.RUNNING);

        StudySession savedSession = studySessionRepository.save(session);

        // Broadcast ranking updates to all groups the user is in
        broadcastToUserGroups(user.getId());

        return savedSession;
    }

    @Transactional // to ensure the session is completed, if error => rollback the transaction
    public StudySession completeStudySession(UUID sessionId, UUID requesterId) {

        StudySession session = studySessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Study session not found"));

        if (!session.getUser().getId().equals(requesterId)) {
            throw new AccessDeniedException("You can only complete your own study session");
        }

        // Completing twice (double click, tab close + pause) must not add the time twice
        if (session.getStatus() != SessionStatus.RUNNING) {
            return session;
        }

        finishSession(session, LocalDateTime.now(clock));
        broadcastToUserGroups(requesterId);

        return session;
    }

    // Mark a RUNNING session completed and add its (capped) duration to every group ranking of the user
    private void finishSession(StudySession session, LocalDateTime now) {
        LocalDateTime endTime = now;
        if (session.getStartTime() != null) {
            LocalDateTime latestEnd = session.getStartTime().plus(MAX_SESSION_DURATION);
            if (endTime.isAfter(latestEnd)) {
                endTime = latestEnd;
            }
        }
        session.setEndTime(endTime);
        session.setStatus(SessionStatus.COMPLETED);

        // Calculate duration if start time is present
        int duration = 0;
        if (session.getStartTime() != null) {
            duration = (int) Math.max(0, Duration.between(session.getStartTime(), endTime).getSeconds());
        }
        session.setDurationSeconds(duration);
        studySessionRepository.save(session);

        // Update group rankings if duration is positive
        if (duration > 0) {
            UserEntity user = session.getUser();
            for (GroupMember member : groupMemberRepository.findAllByUserId(user.getId())) {
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
    }

    // Always broadcast live ranking updates to all groups the user belongs to
    private void broadcastToUserGroups(UUID userId) {
        List<GroupMember> groupMembers = groupMemberRepository.findAllByUserId(userId);
        for (GroupMember member : groupMembers) {
            chatWebSocketHandler.broadcastRanking(member.getGroup().getId());
        }
    }
}
