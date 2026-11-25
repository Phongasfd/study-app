package com.namphong.backend.service;

import com.namphong.backend.dto.AchievementResponse;
import com.namphong.backend.entity.*;
import com.namphong.backend.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Evaluates the achievement catalog for a user and unlocks what they earned.
 *
 * Rules are data (category + threshold in the {@code achievements} table). Each statistic is
 * loaded lazily, at most once per evaluation, and only if a still-locked achievement needs it,
 * so a user who unlocked everything costs one query.
 *
 * Unlocking is {@code INSERT ... ON CONFLICT DO NOTHING}: two evaluations racing for the same
 * user (two instances, two quick sessions) unlock and notify once.
 */
@Service
public class AchievementService {

    /** EARLY_BIRD covers [04:00, threshold:00); NIGHT_OWL covers [threshold:00, 04:00). */
    static final int NIGHT_ENDS_AT_HOUR = 4;

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final StudySessionRepository studySessionRepository;
    private final StudyTaskRepository taskRepository;
    private final StudyGoalRepository goalRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupChallengeRepository challengeRepository;
    private final NotificationService notificationService;
    private final Clock clock;
    private final ZoneId userZone;

    public AchievementService(AchievementRepository achievementRepository,
                              UserAchievementRepository userAchievementRepository,
                              StudySessionRepository studySessionRepository,
                              StudyTaskRepository taskRepository,
                              StudyGoalRepository goalRepository,
                              GroupMemberRepository groupMemberRepository,
                              GroupChallengeRepository challengeRepository,
                              NotificationService notificationService,
                              Clock clock,
                              // session times are stored in server time (UTC); "early" and
                              // "late" only make sense in the users' own time zone
                              @Value("${app.user-zone:Asia/Ho_Chi_Minh}") String userZone) {
        this.achievementRepository = achievementRepository;
        this.userAchievementRepository = userAchievementRepository;
        this.studySessionRepository = studySessionRepository;
        this.taskRepository = taskRepository;
        this.goalRepository = goalRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.challengeRepository = challengeRepository;
        this.notificationService = notificationService;
        this.clock = clock;
        this.userZone = ZoneId.of(userZone);
    }

    /** What the session that triggered the evaluation looked like (null for other triggers). */
    public record SessionFacts(LocalDateTime startTime, int durationSeconds) {
    }

    @Transactional(readOnly = true)
    public List<AchievementResponse> listForUser(UUID userId) {
        Map<String, LocalDateTime> unlocked = unlockedCodes(userId);
        return achievementRepository.findAllByOrderBySortOrderAsc().stream()
                .map(a -> new AchievementResponse(a.getCode(), a.getName(), a.getDescription(), a.getIcon(),
                        a.getCategory(), a.getThreshold(), unlocked.containsKey(a.getCode()),
                        unlocked.get(a.getCode())))
                .toList();
    }

    /** Unlocks every achievement the user now qualifies for; returns the newly unlocked codes. */
    @Transactional
    public List<String> evaluate(UUID userId, SessionFacts lastSession) {
        Map<String, LocalDateTime> unlocked = unlockedCodes(userId);
        List<Achievement> locked = achievementRepository.findAllByOrderBySortOrderAsc().stream()
                .filter(a -> !unlocked.containsKey(a.getCode()))
                .toList();
        if (locked.isEmpty()) {
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        Facts facts = new Facts(userId, today);

        List<String> newlyUnlocked = new ArrayList<>();
        for (Achievement achievement : locked) {
            if (qualifies(achievement, facts, lastSession)
                    && userAchievementRepository.unlock(userId, achievement.getCode(), now) == 1) {
                newlyUnlocked.add(achievement.getCode());
                notificationService.notify(userId, NotificationType.ACHIEVEMENT_UNLOCKED,
                        "Achievement unlocked: " + achievement.getName(), achievement.getName(), "/profile");
            }
        }
        return newlyUnlocked;
    }

    private boolean qualifies(Achievement achievement, Facts facts, SessionFacts lastSession) {
        int threshold = achievement.getThreshold();
        return switch (achievement.getCategory()) {
            case SESSIONS -> facts.completedSessions.get() >= threshold;
            case TOTAL_TIME -> facts.totalSeconds.get() >= threshold;
            case STREAK -> facts.currentStreak.get() >= threshold;
            case TASKS -> facts.tasksDone.get() >= threshold;
            case GROUPS -> facts.groupsJoined.get() >= threshold;
            case DAILY_GOAL -> facts.dailyGoalReached.get();
            case CHALLENGE -> facts.challengeReached.get();
            case SESSION_LENGTH -> lastSession != null && lastSession.durationSeconds() >= threshold;
            case EARLY_BIRD -> lastSession != null && isEarly(localHour(lastSession.startTime()), threshold);
            case NIGHT_OWL -> lastSession != null && isLate(localHour(lastSession.startTime()), threshold);
        };
    }

    static boolean isEarly(int hour, int endsAtHour) {
        return hour >= NIGHT_ENDS_AT_HOUR && hour < endsAtHour;
    }

    static boolean isLate(int hour, int startsAtHour) {
        return hour >= startsAtHour || hour < NIGHT_ENDS_AT_HOUR;
    }

    private int localHour(LocalDateTime serverTime) {
        if (serverTime == null) {
            return 12; // neither early nor late
        }
        return serverTime.atZone(clock.getZone()).withZoneSameInstant(userZone).getHour();
    }

    private Map<String, LocalDateTime> unlockedCodes(UUID userId) {
        return userAchievementRepository.findUnlocked(userId).stream()
                .collect(Collectors.toMap(UserAchievementRepository.Unlocked::getCode,
                        UserAchievementRepository.Unlocked::getUnlockedAt, (a, b) -> a));
    }

    /** The statistics rules look at, each loaded on first use only. */
    private final class Facts {
        final Lazy<Long> completedSessions;
        final Lazy<Long> totalSeconds;
        final Lazy<Integer> currentStreak;
        final Lazy<Long> tasksDone;
        final Lazy<Long> groupsJoined;
        final Lazy<Boolean> dailyGoalReached;
        final Lazy<Boolean> challengeReached;

        Facts(UUID userId, LocalDate today) {
            completedSessions = new Lazy<>(() -> studySessionRepository.countByUserIdAndStatus(userId, SessionStatus.COMPLETED));
            totalSeconds = new Lazy<>(() -> studySessionRepository.sumDurationByUserId(userId, SessionStatus.COMPLETED));
            currentStreak = new Lazy<>(() -> StreakCalculator.compute(studySessionRepository.findStudyDays(userId), today).current());
            tasksDone = new Lazy<>(() -> taskRepository.countByUserIdAndStatus(userId, TaskStatus.DONE));
            groupsJoined = new Lazy<>(() -> groupMemberRepository.countByUserId(userId));
            dailyGoalReached = new Lazy<>(() -> goalRepository.findActiveOverall(userId, GoalPeriod.DAILY)
                    .map(goal -> secondsBetween(userId, today, today) >= goal.getTargetSeconds())
                    .orElse(false));
            challengeReached = new Lazy<>(() -> challengeRepository.findActiveForUser(userId, today).stream()
                    .anyMatch(c -> secondsBetween(userId, c.getStartDate(), c.getEndDate()) >= c.getTargetSeconds()));
        }

        /** Completed seconds of sessions started on days [first, last]. */
        private long secondsBetween(UUID userId, LocalDate first, LocalDate last) {
            return studySessionRepository.sumDurationByUserIds(List.of(userId), SessionStatus.COMPLETED,
                            first.atStartOfDay(), last.plusDays(1).atStartOfDay())
                    .stream()
                    .mapToLong(row -> row.getTotalSeconds() != null ? row.getTotalSeconds() : 0)
                    .sum();
        }
    }

    private static final class Lazy<T> {
        private final Supplier<T> supplier;
        private T value;
        private boolean loaded;

        Lazy(Supplier<T> supplier) {
            this.supplier = supplier;
        }

        T get() {
            if (!loaded) {
                value = supplier.get();
                loaded = true;
            }
            return value;
        }
    }
}
