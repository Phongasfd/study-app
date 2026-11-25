package com.namphong.backend.service;

import com.namphong.backend.entity.*;
import com.namphong.backend.repository.*;
import com.namphong.backend.support.EmbeddedPostgresSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Achievement rules against a real PostgreSQL 16 with the Flyway schema (and its seeded
 * catalog). No test transaction: every call commits, like production.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({AchievementService.class, NotificationService.class, AchievementServiceTest.FixedClock.class})
class AchievementServiceTest {

    private static final int THREADS = 8;
    /** 10:00 UTC = 17:00 in Asia/Ho_Chi_Minh (the default app.user-zone) */
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 5, 10, 0);

    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        }
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> EmbeddedPostgresSupport.freshDatabaseUrl("achievements"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> String.valueOf(THREADS * 2));
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired private AchievementService service;
    @Autowired private UserRepository users;
    @Autowired private StudySessionRepository sessions;
    @Autowired private StudyGoalRepository goals;
    @Autowired private NotificationRepository notifications;
    @Autowired private UserAchievementRepository userAchievements;

    private UserEntity newUser() {
        String name = "u" + UUID.randomUUID().toString().substring(0, 8);
        return users.save(UserEntity.builder().username(name).email(name + "@example.com").build());
    }

    private AchievementService.SessionFacts completed(UserEntity user, LocalDateTime start, int seconds) {
        sessions.save(StudySession.builder().user(user).startTime(start).endTime(start.plusSeconds(seconds))
                .durationSeconds(seconds).status(SessionStatus.COMPLETED).build());
        return new AchievementService.SessionFacts(start, seconds);
    }

    private long notificationsOf(UserEntity user) {
        return notifications.findAll().stream().filter(n -> n.getUser().getId().equals(user.getId())).count();
    }

    @Test
    void unlocksWhatWasEarnedAndNotifiesOnce() {
        UserEntity user = newUser();
        // 07:00 UTC = 14:00 local: neither early nor late
        AchievementService.SessionFacts session = completed(user, NOW.minusHours(3), 7200);

        List<String> first = service.evaluate(user.getId(), session);

        assertTrue(first.containsAll(List.of("FIRST_SESSION", "TOTAL_1H", "MARATHON_2H")), first.toString());
        assertFalse(first.contains("TOTAL_10H"));
        assertFalse(first.contains("EARLY_BIRD"));
        assertFalse(first.contains("NIGHT_OWL"));
        assertFalse(first.contains("GOAL_MET"), "no goal set");
        assertEquals(first.size(), notificationsOf(user), "one notification per unlock");

        assertTrue(service.evaluate(user.getId(), session).isEmpty(), "nothing left to unlock");
        assertEquals(first.size(), notificationsOf(user), "and nothing notified twice");
    }

    @Test
    void streakAndDailyGoal() {
        UserEntity user = newUser();
        completed(user, NOW.minusDays(2), 1800);
        completed(user, NOW.minusDays(1), 1800);
        completed(user, NOW.minusHours(1), 1800);
        StudyGoal goal = new StudyGoal();
        goal.setUser(user);
        goal.setPeriod(GoalPeriod.DAILY);
        goal.setTargetSeconds(1800);
        goal.setCreatedAt(NOW.minusDays(3));
        goals.save(goal);

        List<String> unlocked = service.evaluate(user.getId(), null);

        assertTrue(unlocked.contains("STREAK_3"), unlocked.toString());
        assertFalse(unlocked.contains("STREAK_7"));
        assertTrue(unlocked.contains("GOAL_MET"));
        assertFalse(unlocked.contains("MARATHON_2H"), "session rules need the session that triggered them");
    }

    @Test
    void earlyAndLateAreJudgedInTheUsersTimeZone() {
        UserEntity early = newUser();
        // 23:30 UTC the day before = 06:30 in Vietnam
        assertTrue(service.evaluate(early.getId(), completed(early, LocalDateTime.of(2026, 11, 4, 23, 30), 600))
                .contains("EARLY_BIRD"));

        UserEntity late = newUser();
        // 16:00 UTC = 23:00 in Vietnam
        assertTrue(service.evaluate(late.getId(), completed(late, NOW.minusHours(1).withHour(16).minusDays(1), 600))
                .contains("NIGHT_OWL"));
    }

    @Test
    void concurrentEvaluationsUnlockAndNotifyOnce() throws Exception {
        UserEntity user = newUser();
        AchievementService.SessionFacts session = completed(user, NOW.minusHours(3), 7200);

        // 8 evaluations at the same instant all see "nothing unlocked yet"
        List<List<String>> results = runConcurrently(() -> service.evaluate(user.getId(), session));

        List<String> all = results.stream().flatMap(List::stream).toList();
        assertEquals(new HashSet<>(all).size(), all.size(), "each achievement unlocked by exactly one evaluation");
        assertEquals(all.size(), userAchievements.countByUserId(user.getId()));
        assertEquals(all.size(), notificationsOf(user), "one notification per achievement");
        assertTrue(all.contains("FIRST_SESSION"));
    }

    @Test
    void catalogListsUnlockedState() {
        UserEntity user = newUser();
        service.evaluate(user.getId(), completed(user, NOW.minusHours(3), 600));

        var list = service.listForUser(user.getId());
        assertEquals(16, list.size(), "the catalog seeded by V4");
        assertTrue(list.stream().filter(a -> a.code().equals("FIRST_SESSION")).findFirst().orElseThrow().unlocked());
        assertFalse(list.stream().filter(a -> a.code().equals("TOTAL_100H")).findFirst().orElseThrow().unlocked());
    }

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
        List<T> results = new ArrayList<>();
        for (Future<T> f : futures) {
            results.add(f.get(30, TimeUnit.SECONDS));
        }
        pool.shutdown();
        return results;
    }
}
