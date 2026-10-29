package com.namphong.backend.service;

import com.namphong.backend.entity.*;
import com.namphong.backend.exception.NotFoundException;
import com.namphong.backend.repository.GroupMemberRepository;
import com.namphong.backend.repository.GroupRankingRepository;
import com.namphong.backend.repository.StudySessionRepository;
import com.namphong.backend.websocket.ChatWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StudySessionServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 29, 10, 0);

    private StudySessionRepository studySessionRepository;
    private GroupMemberRepository groupMemberRepository;
    private GroupRankingRepository groupRankingRepository;
    private StudySessionService service;

    private UserEntity owner;
    private StudyGroup group;

    @BeforeEach
    void setUp() {
        studySessionRepository = mock(StudySessionRepository.class);
        groupMemberRepository = mock(GroupMemberRepository.class);
        groupRankingRepository = mock(GroupRankingRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-10-29T10:00:00Z"), ZoneOffset.UTC);
        service = new StudySessionService(studySessionRepository, groupMemberRepository,
                groupRankingRepository, mock(ChatWebSocketHandler.class), clock);

        owner = UserEntity.builder().id(UUID.randomUUID()).username("owner").build();
        group = new StudyGroup();
        group.setId(UUID.randomUUID());
        GroupMember membership = new GroupMember();
        membership.setGroup(group);
        membership.setUser(owner);
        when(groupMemberRepository.findAllByUserId(owner.getId())).thenReturn(List.of(membership));
        when(groupRankingRepository.findByGroupIdAndUserId(any(), any())).thenReturn(Optional.empty());
        when(studySessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private StudySession runningSession(LocalDateTime startTime) {
        StudySession session = StudySession.builder()
                .id(UUID.randomUUID())
                .user(owner)
                .startTime(startTime)
                .status(SessionStatus.RUNNING)
                .build();
        when(studySessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        return session;
    }

    private int creditedSeconds() {
        ArgumentCaptor<GroupRanking> captor = ArgumentCaptor.forClass(GroupRanking.class);
        verify(groupRankingRepository).save(captor.capture());
        return captor.getValue().getTotalDuration();
    }

    @Test
    void create_usesServerTime() {
        when(studySessionRepository.findFirstByUserIdAndStatusOrderByStartTimeDesc(owner.getId(), SessionStatus.RUNNING))
                .thenReturn(Optional.empty());

        StudySession session = service.createStudySession(owner);

        assertEquals(NOW, session.getStartTime());
        assertEquals(SessionStatus.RUNNING, session.getStatus());
    }

    @Test
    void create_closesRunningSessionAndCreditsRanking() {
        StudySession old = runningSession(NOW.minusMinutes(30));
        when(studySessionRepository.findFirstByUserIdAndStatusOrderByStartTimeDesc(owner.getId(), SessionStatus.RUNNING))
                .thenReturn(Optional.of(old));

        service.createStudySession(owner);

        assertEquals(SessionStatus.COMPLETED, old.getStatus());
        assertEquals(30 * 60, creditedSeconds());
    }

    @Test
    void complete_creditsElapsedServerTime() {
        StudySession session = runningSession(NOW.minusMinutes(25));

        StudySession completed = service.completeStudySession(session.getId(), owner.getId());

        assertEquals(SessionStatus.COMPLETED, completed.getStatus());
        assertEquals(NOW, completed.getEndTime());
        assertEquals(25 * 60, completed.getDurationSeconds());
        assertEquals(25 * 60, creditedSeconds());
    }

    @Test
    void complete_rejectsSomeoneElsesSession() {
        StudySession session = runningSession(NOW.minusMinutes(25));

        assertThrows(AccessDeniedException.class,
                () -> service.completeStudySession(session.getId(), UUID.randomUUID()));
        assertEquals(SessionStatus.RUNNING, session.getStatus());
        verify(groupRankingRepository, never()).save(any());
    }

    @Test
    void complete_isIdempotent() {
        StudySession session = runningSession(NOW.minusMinutes(25));
        service.completeStudySession(session.getId(), owner.getId());
        reset(groupRankingRepository);

        service.completeStudySession(session.getId(), owner.getId());

        verify(groupRankingRepository, never()).save(any());
    }

    @Test
    void complete_capsForgottenTimerAtTwelveHours() {
        StudySession session = runningSession(NOW.minusDays(3));

        StudySession completed = service.completeStudySession(session.getId(), owner.getId());

        assertEquals(12 * 60 * 60, completed.getDurationSeconds());
        assertEquals(session.getStartTime().plusHours(12), completed.getEndTime());
    }

    @Test
    void complete_unknownSession() {
        UUID id = UUID.randomUUID();
        when(studySessionRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.completeStudySession(id, owner.getId()));
    }
}
