package com.namphong.backend.service;

import com.namphong.backend.entity.*;
import com.namphong.backend.repository.*;
import com.namphong.backend.support.EmbeddedPostgresSupport;
import com.namphong.backend.websocket.GroupEventBus;
import com.namphong.backend.websocket.RankingBroadcaster;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Fires the same request from 8 threads at once against a real PostgreSQL 16, the way a
 * double click or two open tabs do. Each call commits on its own (no test transaction),
 * so the database's row locks and unique index are what is actually being tested.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED) // real commits, like production
@Import({StudySessionService.class, GroupRankingService.class, RankingBroadcaster.class,
        StudySessionConcurrencyTest.FixedClock.class})
class StudySessionConcurrencyTest {

    private static final int THREADS = 8;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 5, 10, 0);

    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-11-05T10:00:00Z"), ZoneOffset.UTC);
        }
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> EmbeddedPostgresSupport.freshDatabaseUrl("concurrency"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> String.valueOf(THREADS * 2));
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired private StudySessionService service;
    @Autowired private StudySessionRepository sessions;
    @Autowired private GroupRankingRepository rankings;
    @Autowired private UserRepository users;
    @Autowired private GroupRepository groups;
    @Autowired private GroupMemberRepository members;
    @Autowired private PlatformTransactionManager transactionManager;
    @MockitoBean private GroupEventBus groupEventBus; // what RankingBroadcaster pushes to the sockets

    /** Ranking pushes are AFTER_COMMIT: one per transaction that committed a change, none for rollbacks. */
    private void verifyRankingPushedOnce() {
        verify(groupEventBus, timeout(2_000)).publish(eq(group.getId()), eq("ranking"), any());
        verify(groupEventBus, after(300).times(1)).publish(any(), any(), any());
    }

    private UserEntity user;
    private StudyGroup group;

    @BeforeEach
    void setUp() {
        String name = "u" + UUID.randomUUID().toString().substring(0, 8);
        user = users.save(UserEntity.builder().username(name).email(name + "@example.com").build());
        group = new StudyGroup();
        group.setName("g");
        group.setOwner(user);
        group.setMaxMembers(10);
        group = groups.save(group);
        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(user);
        member.setJoinedAt(NOW);
        members.save(member);
    }

    private StudySession runningSince(LocalDateTime start) {
        return sessions.save(StudySession.builder().user(user).startTime(start).status(SessionStatus.RUNNING).build());
    }

    private int rankedSeconds() {
        return rankings.findByGroupIdAndUserId(group.getId(), user.getId())
                .map(GroupRanking::getTotalDuration).orElse(0);
    }

    private long runningSessionsOfUser() {
        return sessions.findAll().stream()
                .filter(s -> s.getUser().getId().equals(user.getId()) && s.getStatus() == SessionStatus.RUNNING)
                .count();
    }

    /** Starts all tasks at the same instant and returns their results (rethrowing any failure). */
    private static <T> List<T> runConcurrently(Callable<T> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<T>> futures = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                return task.call();
            }));
        }
        ready.await();
        go.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS), "requests deadlocked or hung");
        List<T> results = new ArrayList<>();
        for (Future<T> f : futures) {
            results.add(f.get());
        }
        return results;
    }

    @Test
    void concurrentCompletesOfOneSessionCreditItOnce() throws Exception {
        StudySession session = runningSince(NOW.minusMinutes(30));

        List<StudySession> results = runConcurrently(
                () -> service.completeStudySession(session.getId(), user.getId()));

        assertTrue(results.stream().allMatch(s -> s.getStatus() == SessionStatus.COMPLETED),
                "every caller sees the session as completed");
        assertEquals(30 * 60, rankedSeconds(), "30 minutes credited once, not once per request");
        StudySession stored = sessions.findById(session.getId()).orElseThrow();
        assertEquals(30 * 60, stored.getDurationSeconds());
        verifyRankingPushedOnce();
    }

    @Test
    void concurrentStartsConvergeOnOneRunningSession() throws Exception {
        // a session is already running: every start tries to close it and open a new one
        runningSince(NOW.minusMinutes(10));

        List<StudySession> results = runConcurrently(() -> service.createStudySession(user));

        // Callers that overlapped the winner get the winner's session back. A thread that only
        // started after the winner committed legitimately restarts (like pressing start again),
        // so the invariants are about the database, not about every caller getting one id.
        Set<UUID> committedStarts = results.stream().map(StudySession::getId).collect(Collectors.toSet());
        assertEquals(1, runningSessionsOfUser(), "the database holds exactly one RUNNING session");
        assertTrue(committedStarts.size() < THREADS, "overlapping starts converged instead of each creating one");
        assertEquals(10 * 60, rankedSeconds(), "the previous session is credited once");
        // one push per committed start; the rolled-back ones broadcast nothing
        verify(groupEventBus, timeout(2_000).times(committedStarts.size())).publish(eq(group.getId()), eq("ranking"), any());
        verify(groupEventBus, after(300).times(committedStarts.size())).publish(any(), any(), any());
    }

    @Test
    void concurrentRankingIncrementsAreNotLost() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        runConcurrently(() -> tx.execute(status -> rankings.addDuration(group.getId(), user.getId(), 60)));

        // a read-modify-write in Java would lose increments here (and race on the first insert)
        assertEquals(THREADS * 60, rankedSeconds());
    }
}
