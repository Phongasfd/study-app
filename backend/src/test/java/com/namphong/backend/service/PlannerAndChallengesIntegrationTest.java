package com.namphong.backend.service;

import com.namphong.backend.dto.*;
import com.namphong.backend.entity.*;
import com.namphong.backend.exception.NotFoundException;
import com.namphong.backend.repository.*;
import com.namphong.backend.support.EmbeddedPostgresSupport;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.*;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Goals, tasks, notifications and group challenges on a real PostgreSQL 16 (schema from Flyway),
 * including statement count checks for challenge detail (constant query count regardless of group size).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({GoalService.class, TaskService.class, NotificationService.class, ChallengeService.class,
        GroupMemberService.class, ProfileService.class, PlannerAndChallengesIntegrationTest.FixedClock.class})
class PlannerAndChallengesIntegrationTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 25); // Wednesday
    private static final LocalDateTime NOW = TODAY.atTime(10, 0);

    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-11-25T10:00:00Z"), ZoneOffset.UTC);
        }
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> EmbeddedPostgresSupport.freshDatabaseUrl("planner"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.properties.hibernate.generate_statistics", () -> "true");
    }

    @Autowired private TestEntityManager em;
    @Autowired private GoalService goalService;
    @Autowired private TaskService taskService;
    @Autowired private NotificationService notificationService;
    @Autowired private ChallengeService challengeService;
    @Autowired private ProfileService profileService;
    @Autowired private StudyGoalRepository goalRepository;
    @Autowired private StudySessionRepository sessionRepository;
    @Autowired private SessionSubjectRepository sessionSubjectRepository;
    @Autowired private SubjectRepository subjectRepository;

    private Statistics statistics;
    private UserEntity user;
    private UserEntity other;
    private Subject math;

    @BeforeEach
    void setUp() {
        statistics = em.getEntityManager().getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        user = persistUser("alice");
        other = persistUser("bob");
        math = new Subject();
        math.setUser(user);
        math.setName("Math");
        math = subjectRepository.save(math);
    }

    private UserEntity persistUser(String prefix) {
        String name = prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
        return em.persist(UserEntity.builder().username(name).email(name + "@example.com").build());
    }

    private StudySession completedSession(UserEntity owner, LocalDateTime start, int seconds) {
        StudySession s = StudySession.builder()
                .user(owner)
                .startTime(start)
                .endTime(start.plusSeconds(seconds))
                .durationSeconds(seconds)
                .status(SessionStatus.COMPLETED)
                .build();
        return sessionRepository.save(s);
    }

    @Test
    void settingAGoalReplacesTheActiveOneAndComputesProgress() {
        StudySession todaySession = completedSession(user, NOW.minusHours(2), 1800);
        SessionSubject link = new SessionSubject();
        link.setSession(todaySession);
        link.setSubject(math);
        sessionSubjectRepository.save(link);

        GoalProgressResponse first = goalService.create(user.getId(), new GoalRequest(null, GoalPeriod.DAILY, 60));
        assertEquals(3600, first.targetSeconds());
        assertEquals(1800, first.progressSeconds());
        assertEquals(50, first.percent());
        assertFalse(first.achieved());

        // Replacing the daily overall goal deactivates the previous row and creates a new active one
        GoalProgressResponse second = goalService.create(user.getId(), new GoalRequest(null, GoalPeriod.DAILY, 30));
        assertNotEquals(first.id(), second.id());
        assertTrue(second.achieved());
        assertEquals(1, goalService.listActive(user.getId()).size(), "only one active overall DAILY goal");
        assertFalse(goalRepository.findById(first.id()).orElseThrow().isActive(), "previous goal kept as history");

        // Subject goal on Math
        GoalProgressResponse mathGoal = goalService.create(user.getId(),
                new GoalRequest(math.getId(), GoalPeriod.WEEKLY, 60));
        assertEquals("Math", mathGoal.subjectName());
        assertEquals(1800, mathGoal.progressSeconds());
        assertEquals(2, goalService.listActive(user.getId()).size());
    }

    @Test
    void tasksAreScopedToOwnerAndSummaryCountsStatusesAndDueDates() {
        TaskResponse overdue = taskService.create(user.getId(),
                new TaskRequest("Past homework", "Ch. 1", math.getId(), TaskPriority.HIGH, TaskStatus.TODO, TODAY.minusDays(1), 45));
        TaskResponse dueToday = taskService.create(user.getId(),
                new TaskRequest("Today quiz", null, null, TaskPriority.MEDIUM, TaskStatus.IN_PROGRESS, TODAY, 30));
        TaskResponse done = taskService.create(user.getId(),
                new TaskRequest("Read syllabus", null, null, TaskPriority.LOW, TaskStatus.DONE, TODAY.minusDays(2), 15));

        assertTrue(overdue.overdue());
        assertFalse(dueToday.overdue());
        assertFalse(done.overdue());
        assertNotNull(done.completedAt());

        TaskSummaryResponse summary = taskService.summary(user.getId());
        assertEquals(1, summary.todo());
        assertEquals(1, summary.inProgress());
        assertEquals(1, summary.done());
        assertEquals(1, summary.overdue());
        assertEquals(1, summary.dueToday());

        assertEquals(1, taskService.list(user.getId(), null, null, "overdue").size());
        assertEquals(1, taskService.list(user.getId(), null, math.getId(), null).size());

        // Another user cannot touch this user's task
        assertThrows(NotFoundException.class,
                () -> taskService.changeStatus(other.getId(), overdue.id(), TaskStatus.DONE));
    }

    @Test
    void notificationsSupportUnreadCountMarkReadAndRetentionCleanup() {
        notificationService.notify(user.getId(), NotificationType.ACHIEVEMENT_UNLOCKED, "Unlocked!", "First Step", "/profile");
        notificationService.notify(user.getId(), NotificationType.CHALLENGE_CREATED, "Sprint", "10h week", "/groups/1");

        assertEquals(2, notificationService.unreadCount(user.getId()));
        NotificationPageResponse page = notificationService.list(user.getId(), 0, 10);
        assertEquals(2, page.items().size());
        assertEquals(2, page.unread());

        UUID firstId = page.items().getFirst().id();
        assertThrows(NotFoundException.class, () -> notificationService.markRead(firstId, other.getId()));

        notificationService.markRead(firstId, user.getId());
        assertEquals(1, notificationService.unreadCount(user.getId()));

        notificationService.markAllRead(user.getId());
        assertEquals(0, notificationService.unreadCount(user.getId()));
    }

    @Test
    void challengeBoardUsesConstantQueryCountRegardlessOfGroupSize() {
        long small = statementsForChallengeDetail(3);
        long large = statementsForChallengeDetail(15);

        assertEquals(small, large, "challenge detail must not run N+1 queries per member");
        assertTrue(large <= 4, "expected at most 4 statements, got " + large);
    }

    private long statementsForChallengeDetail(int memberCount) {
        UserEntity owner = persistUser("owner");
        StudyGroup group = new StudyGroup();
        group.setName("g-" + memberCount);
        group.setOwner(owner);
        group.setMaxMembers(50);
        group.setVisibility(GroupVisibility.PUBLIC);
        em.persist(group);

        for (int i = 0; i < memberCount; i++) {
            UserEntity memberUser = i == 0 ? owner : persistUser("m" + i);
            GroupMember gm = new GroupMember();
            gm.setGroup(group);
            gm.setUser(memberUser);
            gm.setJoinedAt(NOW);
            em.persist(gm);
            completedSession(memberUser, NOW.minusHours(2), (i + 1) * 600);
        }
        GroupChallenge challenge = new GroupChallenge();
        challenge.setGroup(group);
        challenge.setCreatedBy(owner);
        challenge.setTitle("Finals Week");
        challenge.setTargetSeconds(3600);
        challenge.setStartDate(TODAY.minusDays(1));
        challenge.setEndDate(TODAY.plusDays(5));
        challenge.setCreatedAt(NOW);
        em.persist(challenge);
        em.flush();
        em.clear();

        statistics.clear();
        ChallengeDetailResponse detail = challengeService.detail(group.getId(), challenge.getId(), owner.getId());
        long count = statistics.getPrepareStatementCount();
        assertEquals(memberCount, detail.participants().size());
        assertEquals(1, detail.participants().getFirst().rank());
        em.clear();
        return count;
    }

    @Test
    void profileSummaryAggregatesStreakSessionsTasksAndGroups() {
        completedSession(user, NOW.minusDays(1), 1800);
        completedSession(user, NOW.minusHours(1), 3600);
        taskService.create(user.getId(),
                new TaskRequest("Done task", null, null, TaskPriority.MEDIUM, TaskStatus.DONE, TODAY, 20));

        ProfileSummaryResponse profile = profileService.summary(user.getId());
        assertEquals(5400, profile.totalSeconds());
        assertEquals(2, profile.completedSessions());
        assertEquals(2, profile.currentStreak());
        assertEquals(2, profile.longestStreak());
        assertEquals(1, profile.tasksDone());
        assertEquals(16, profile.achievementsTotal());
    }
}
