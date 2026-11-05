package com.namphong.backend.service;

import com.namphong.backend.entity.*;
import com.namphong.backend.exception.NotFoundException;
import com.namphong.backend.repository.GroupMemberRepository;
import com.namphong.backend.repository.GroupRankingRepository;
import com.namphong.backend.repository.StudySessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Business rules with mocked repositories. The real concurrency behaviour is in StudySessionConcurrencyTest. */
class StudySessionServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 29, 10, 0);

    private StudySessionRepository studySessionRepository;
    private GroupMemberRepository groupMemberRepository;
    private GroupRankingRepository groupRankingRepository;
    private ApplicationEventPublisher events;
    private StudySessionService service;

    private UserEntity owner;
    private StudyGroup group;

    @BeforeEach
    void setUp() {
        studySessionRepository = mock(StudySessionRepository.class);
        groupMemberRepository = mock(GroupMemberRepository.class);
        groupRankingRepository = mock(GroupRankingRepository.class);
        events = mock(ApplicationEventPublisher.class);
        Clock clock = Clock.fixed(Instant.parse("2026-10-29T10:00:00Z"), ZoneOffset.UTC);
        service = new StudySessionService(studySessionRepository, groupMemberRepository,
                groupRankingRepository, events, clock, TransactionOperations.withoutTransaction());

        owner = UserEntity.builder().id(UUID.randomUUID()).username("owner").build();
        group = new StudyGroup();
        group.setId(UUID.randomUUID());
        GroupMember membership = new GroupMember();
        membership.setGroup(group);
        membership.setUser(owner);
        when(groupMemberRepository.findAllByUserId(owner.getId())).thenReturn(List.of(membership));
        when(studySessionRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        // the default method delegates to the 5-arg query; with a mock, stub the 3-arg one directly
        when(studySessionRepository.completeIfRunning(any(UUID.class), any(LocalDateTime.class), anyInt())).thenReturn(1);
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

    private void verifyCredited(int seconds) {
        verify(groupRankingRepository).addDuration(group.getId(), owner.getId(), seconds);
    }

    @Test
    void create_usesServerTime() {
        when(studySessionRepository.findFirstByUserIdAndStatusOrderByStartTimeDesc(owner.getId(), SessionStatus.RUNNING))
                .thenReturn(Optional.empty());

        StudySession session = service.createStudySession(owner);

        assertEquals(NOW, session.getStartTime());
        assertEquals(SessionStatus.RUNNING, session.getStatus());
        verify(events).publishEvent(new StudySessionChangedEvent(owner.getId()));
    }

    @Test
    void create_closesRunningSessionAndCreditsRanking() {
        StudySession old = runningSession(NOW.minusMinutes(30));
        when(studySessionRepository.findFirstByUserIdAndStatusOrderByStartTimeDesc(owner.getId(), SessionStatus.RUNNING))
                .thenReturn(Optional.of(old));

        service.createStudySession(owner);

        assertEquals(SessionStatus.COMPLETED, old.getStatus());
        verify(studySessionRepository).completeIfRunning(old.getId(), NOW, 30 * 60);
        verifyCredited(30 * 60);
    }

    @Test
    void create_whenAConcurrentStartWon_returnsThatSession() {
        StudySession winner = StudySession.builder().id(UUID.randomUUID()).user(owner)
                .startTime(NOW).status(SessionStatus.RUNNING).build();
        when(studySessionRepository.findFirstByUserIdAndStatusOrderByStartTimeDesc(owner.getId(), SessionStatus.RUNNING))
                .thenReturn(Optional.empty())       // our transaction: nothing running yet
                .thenReturn(Optional.of(winner));  // fallback read: the other request's session
        when(studySessionRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"" + StudySessionRepository.ONE_RUNNING_SESSION_INDEX + "\""));

        assertSame(winner, service.createStudySession(owner));
    }

    @Test
    void create_otherIntegrityErrorsAreNotSwallowed() {
        when(studySessionRepository.findFirstByUserIdAndStatusOrderByStartTimeDesc(owner.getId(), SessionStatus.RUNNING))
                .thenReturn(Optional.empty());
        when(studySessionRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("fk violation"));

        assertThrows(DataIntegrityViolationException.class, () -> service.createStudySession(owner));
    }

    @Test
    void complete_creditsElapsedServerTime() {
        StudySession session = runningSession(NOW.minusMinutes(25));

        StudySession completed = service.completeStudySession(session.getId(), owner.getId());

        assertEquals(SessionStatus.COMPLETED, completed.getStatus());
        assertEquals(NOW, completed.getEndTime());
        assertEquals(25 * 60, completed.getDurationSeconds());
        verifyCredited(25 * 60);
        verify(events).publishEvent(new StudySessionChangedEvent(owner.getId()));
    }

    @Test
    void complete_rejectsSomeoneElsesSession() {
        StudySession session = runningSession(NOW.minusMinutes(25));

        assertThrows(AccessDeniedException.class,
                () -> service.completeStudySession(session.getId(), UUID.randomUUID()));
        assertEquals(SessionStatus.RUNNING, session.getStatus());
        verify(studySessionRepository, never()).completeIfRunning(any(UUID.class), any(LocalDateTime.class), anyInt());
        verify(groupRankingRepository, never()).addDuration(any(), any(), anyInt());
    }

    @Test
    void complete_isIdempotent() {
        StudySession session = runningSession(NOW.minusMinutes(25));
        service.completeStudySession(session.getId(), owner.getId());
        reset(groupRankingRepository);

        service.completeStudySession(session.getId(), owner.getId());

        verify(groupRankingRepository, never()).addDuration(any(), any(), anyInt());
    }

    @Test
    void complete_lostRace_creditsNothing() {
        // both requests read RUNNING; the other one's UPDATE matched first
        StudySession session = runningSession(NOW.minusMinutes(25));
        when(studySessionRepository.completeIfRunning(eq(session.getId()), any(LocalDateTime.class), anyInt())).thenReturn(0);

        service.completeStudySession(session.getId(), owner.getId());

        verify(groupRankingRepository, never()).addDuration(any(), any(), anyInt());
        verify(events, never()).publishEvent(any(Object.class));
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
