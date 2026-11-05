package com.namphong.backend.service;

import com.namphong.backend.entity.*;
import com.namphong.backend.exception.NotFoundException;
import com.namphong.backend.repository.GroupMemberRepository;
import com.namphong.backend.repository.GroupRankingRepository;
import com.namphong.backend.repository.StudySessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class StudySessionService {
    // A forgotten timer must not add days of study time to the ranking
    public static final Duration MAX_SESSION_DURATION = Duration.ofHours(12);

    private final StudySessionRepository studySessionRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupRankingRepository groupRankingRepository;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final TransactionOperations transactions;

    @Autowired
    public StudySessionService(StudySessionRepository studySessionRepository,
                               GroupMemberRepository groupMemberRepository,
                               GroupRankingRepository groupRankingRepository,
                               ApplicationEventPublisher events,
                               Clock clock,
                               PlatformTransactionManager transactionManager) {
        this(studySessionRepository, groupMemberRepository, groupRankingRepository, events, clock,
                new TransactionTemplate(transactionManager));
    }

    // Unit tests pass TransactionOperations.withoutTransaction()
    StudySessionService(StudySessionRepository studySessionRepository,
                        GroupMemberRepository groupMemberRepository,
                        GroupRankingRepository groupRankingRepository,
                        ApplicationEventPublisher events,
                        Clock clock,
                        TransactionOperations transactions) {
        this.studySessionRepository = studySessionRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.groupRankingRepository = groupRankingRepository;
        this.events = events;
        this.clock = clock;
        this.transactions = transactions;
    }

    /** A RUNNING session older than the cap is a forgotten timer, not someone studying. */
    public static boolean isAbandoned(LocalDateTime startTime, LocalDateTime now) {
        return Duration.between(startTime, now).compareTo(MAX_SESSION_DURATION) > 0;
    }

    /**
     * Starts a session (closing and crediting a running one first).
     *
     * Two starts at the same moment (double click, two tabs) used to create two RUNNING
     * sessions whose overlapping time was later counted twice. The database now allows one
     * RUNNING session per user (V3 partial unique index), so the slower request fails on
     * insert; it then returns the session the faster request created. Concurrent starts
     * therefore converge on a single RUNNING session.
     *
     * Not @Transactional on purpose: the failed insert aborts its transaction, so the
     * fallback read needs a transaction of its own.
     */
    public StudySession createStudySession(UserEntity user) {
        try {
            return transactions.execute(status -> startNewSession(user));
        } catch (DataIntegrityViolationException e) {
            if (!violates(e, StudySessionRepository.ONE_RUNNING_SESSION_INDEX)) {
                throw e;
            }
            return transactions.execute(status -> studySessionRepository
                    .findFirstByUserIdAndStatusOrderByStartTimeDesc(user.getId(), SessionStatus.RUNNING)
                    // the winner was already stopped again: nothing to converge on, report the conflict
                    .orElseThrow(() -> e));
        }
    }

    // Start/end times always come from the server clock: client-sent times
    // let anyone backdate a session and inflate their ranking.
    private StudySession startNewSession(UserEntity user) {
        LocalDateTime now = LocalDateTime.now(clock);

        // Check if user already has a running session -> close it (and credit its time) first
        studySessionRepository.findFirstByUserIdAndStatusOrderByStartTimeDesc(user.getId(), SessionStatus.RUNNING)
                .ifPresent(oldSession -> finishSession(oldSession, now));

        StudySession session = new StudySession();
        session.setUser(user);
        session.setStartTime(now);
        session.setStatus(SessionStatus.RUNNING);

        // flush now so a violation of the one-RUNNING-session index surfaces here, not at commit
        StudySession savedSession = studySessionRepository.saveAndFlush(session);

        // Ranking updates are pushed after commit (RankingBroadcaster)
        events.publishEvent(new StudySessionChangedEvent(user.getId()));

        return savedSession;
    }

    @Transactional // to ensure the session is completed, if error => rollback the transaction
    public StudySession completeStudySession(UUID sessionId, UUID requesterId) {

        StudySession session = studySessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Study session not found"));

        if (!session.getUser().getId().equals(requesterId)) {
            throw new AccessDeniedException("You can only complete your own study session");
        }

        // Completing twice (double click, tab close + pause) must not add the time twice.
        // This check is only a shortcut: two requests can both pass it at the same time,
        // so finishSession() decides the winner atomically in the database.
        if (session.getStatus() != SessionStatus.RUNNING) {
            return session;
        }

        if (!finishSession(session, LocalDateTime.now(clock))) {
            // a concurrent request completed it between our read and our update
            return studySessionRepository.findById(sessionId).orElse(session);
        }
        events.publishEvent(new StudySessionChangedEvent(requesterId));

        return session;
    }

    /**
     * Marks a RUNNING session completed and adds its (capped) duration to every group ranking
     * of the user. Returns false, crediting nothing, if another request already completed it.
     */
    private boolean finishSession(StudySession session, LocalDateTime now) {
        LocalDateTime endTime = now;
        if (session.getStartTime() != null) {
            LocalDateTime latestEnd = session.getStartTime().plus(MAX_SESSION_DURATION);
            if (endTime.isAfter(latestEnd)) {
                endTime = latestEnd;
            }
        }

        // Calculate duration if start time is present
        int duration = 0;
        if (session.getStartTime() != null) {
            duration = (int) Math.max(0, Duration.between(session.getStartTime(), endTime).getSeconds());
        }

        // Only the request whose conditional UPDATE matched may credit the time
        if (studySessionRepository.completeIfRunning(session.getId(), endTime, duration) == 0) {
            return false;
        }
        // reflect the update on the returned object (it is detached, so this writes nothing)
        session.setEndTime(endTime);
        session.setStatus(SessionStatus.COMPLETED);
        session.setDurationSeconds(duration);

        // Update group rankings if duration is positive (atomic increment in SQL)
        if (duration > 0) {
            UUID userId = session.getUser().getId();
            for (GroupMember member : groupMemberRepository.findAllByUserId(userId)) {
                groupRankingRepository.addDuration(member.getGroup().getId(), userId, duration);
            }
        }
        return true;
    }

    private static boolean violates(DataIntegrityViolationException e, String constraintName) {
        Throwable cause = e.getMostSpecificCause();
        return cause.getMessage() != null && cause.getMessage().contains(constraintName);
    }
}
