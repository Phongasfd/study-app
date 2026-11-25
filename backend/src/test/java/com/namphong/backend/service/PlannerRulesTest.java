package com.namphong.backend.service;

import com.namphong.backend.entity.GoalPeriod;
import com.namphong.backend.entity.GroupChallenge;
import com.namphong.backend.entity.StudyTask;
import com.namphong.backend.entity.TaskPriority;
import com.namphong.backend.entity.TaskStatus;
import com.namphong.backend.exception.BadRequestException;
import com.namphong.backend.repository.StudySessionRepository.SessionSlice;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Pure rules of goals, tasks, challenges and achievements: no database. */
class PlannerRulesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 25); // a Wednesday
    private static final LocalDateTime NOW = TODAY.atTime(10, 0);

    record Slice(UUID getId, LocalDateTime getStartTime, Integer getDurationSeconds) implements SessionSlice {
    }

    @Test
    void periodsStartOnMondayAndOnTheFirstOfTheMonth() {
        assertEquals(TODAY, GoalPeriod.DAILY.start(TODAY));
        assertEquals(TODAY.plusDays(1), GoalPeriod.DAILY.endExclusive(TODAY));
        assertEquals(LocalDate.of(2026, 11, 23), GoalPeriod.WEEKLY.start(TODAY));
        assertEquals(LocalDate.of(2026, 11, 30), GoalPeriod.WEEKLY.endExclusive(TODAY));
        assertEquals(LocalDate.of(2026, 11, 1), GoalPeriod.MONTHLY.start(TODAY));
        assertEquals(LocalDate.of(2026, 12, 1), GoalPeriod.MONTHLY.endExclusive(TODAY));
    }

    @Test
    void goalProgressCountsOnlyTheWindowAndSplitsMultiSubjectSessions() {
        UUID math = UUID.randomUUID();
        UUID physics = UUID.randomUUID();
        Slice today = new Slice(UUID.randomUUID(), NOW.minusHours(2), 3600);
        Slice todayTwoSubjects = new Slice(UUID.randomUUID(), NOW.minusHours(1), 1800);
        Slice yesterday = new Slice(UUID.randomUUID(), NOW.minusDays(1), 7200);
        List<SessionSlice> sessions = List.of(today, todayTwoSubjects, yesterday);
        Map<UUID, List<UUID>> subjects = Map.of(
                today.getId(), List.of(math),
                todayTwoSubjects.getId(), List.of(math, physics),
                yesterday.getId(), List.of(math));

        LocalDateTime from = TODAY.atStartOfDay();
        LocalDateTime to = TODAY.plusDays(1).atStartOfDay();
        assertEquals(5400, GoalService.progressSeconds(sessions, subjects, null, from, to), "all of today");
        assertEquals(3600 + 900, GoalService.progressSeconds(sessions, subjects, math, from, to), "half of the shared one");
        assertEquals(900, GoalService.progressSeconds(sessions, subjects, physics, from, to));
    }

    private static StudyTask task(String title, TaskStatus status, TaskPriority priority, LocalDate due) {
        StudyTask t = new StudyTask();
        t.setTitle(title);
        t.setPriority(priority);
        t.setDueDate(due);
        t.setCreatedAt(NOW.minusDays(5));
        t.transitionTo(status, NOW.minusDays(1));
        return t;
    }

    @Test
    void openTasksComeFirstOverdueOnTopThenByDueDateThenPriority() {
        List<StudyTask> tasks = new ArrayList<>(List.of(
                task("done", TaskStatus.DONE, TaskPriority.HIGH, TODAY.minusDays(3)),
                task("later-low", TaskStatus.TODO, TaskPriority.LOW, TODAY.plusDays(3)),
                task("undated", TaskStatus.TODO, TaskPriority.HIGH, null),
                task("overdue", TaskStatus.IN_PROGRESS, TaskPriority.LOW, TODAY.minusDays(1)),
                task("later-high", TaskStatus.TODO, TaskPriority.HIGH, TODAY.plusDays(3))));

        tasks.sort(TaskService.displayOrder(TODAY));

        assertEquals(List.of("overdue", "later-high", "later-low", "undated", "done"),
                tasks.stream().map(StudyTask::getTitle).toList());
    }

    @Test
    void completedAtFollowsTheStatus() {
        StudyTask t = task("t", TaskStatus.TODO, TaskPriority.MEDIUM, null);
        assertNull(t.getCompletedAt());

        assertTrue(t.transitionTo(TaskStatus.DONE, NOW), "TODO -> DONE is a completion");
        assertEquals(NOW, t.getCompletedAt());
        assertFalse(t.transitionTo(TaskStatus.DONE, NOW.plusHours(1)), "DONE -> DONE is not a second completion");
        assertEquals(NOW, t.getCompletedAt(), "and keeps the first completion time");

        assertFalse(t.transitionTo(TaskStatus.IN_PROGRESS, NOW.plusHours(2)));
        assertNull(t.getCompletedAt(), "reopened");
    }

    @Test
    void doneTasksAreNeverOverdue() {
        assertTrue(task("a", TaskStatus.TODO, TaskPriority.LOW, TODAY.minusDays(1)).isOverdue(TODAY));
        assertFalse(task("b", TaskStatus.DONE, TaskPriority.LOW, TODAY.minusDays(1)).isOverdue(TODAY));
        assertFalse(task("c", TaskStatus.TODO, TaskPriority.LOW, TODAY).isOverdue(TODAY), "due today is not late yet");
    }

    @Test
    void unknownDueFilterIsABadRequest() {
        assertEquals(TaskService.DueFilter.OVERDUE, TaskService.parseDue("overdue"));
        assertEquals(TaskService.DueFilter.NONE, TaskService.parseDue(null));
        assertThrows(BadRequestException.class, () -> TaskService.parseDue("tomorrow"));
    }

    @Test
    void challengeDatesAreValidated() {
        assertDoesNotThrow(() -> ChallengeService.validateDates(TODAY, TODAY.plusDays(6), TODAY));
        assertDoesNotThrow(() -> ChallengeService.validateDates(TODAY.minusDays(2), TODAY, TODAY));
        assertThrows(BadRequestException.class, () -> ChallengeService.validateDates(TODAY, TODAY.minusDays(1), TODAY));
        assertThrows(BadRequestException.class, () -> ChallengeService.validateDates(TODAY.minusDays(9), TODAY.minusDays(1), TODAY));
        assertThrows(BadRequestException.class, () -> ChallengeService.validateDates(TODAY.minusDays(31), TODAY, TODAY));
        assertThrows(BadRequestException.class, () -> ChallengeService.validateDates(TODAY, TODAY.plusDays(90), TODAY));
    }

    @Test
    void challengeProgressIncludesBothEndDays() {
        GroupChallenge c = new GroupChallenge();
        c.setStartDate(TODAY);
        c.setEndDate(TODAY.plusDays(2));
        assertEquals(GroupChallenge.Status.UPCOMING, c.statusOn(TODAY.minusDays(1)));
        assertEquals(GroupChallenge.Status.ACTIVE, c.statusOn(TODAY));
        assertEquals(GroupChallenge.Status.ENDED, c.statusOn(TODAY.plusDays(3)));

        List<SessionSlice> sessions = List.of(
                new Slice(UUID.randomUUID(), TODAY.atStartOfDay(), 100),
                new Slice(UUID.randomUUID(), TODAY.plusDays(2).atTime(23, 59), 200),
                new Slice(UUID.randomUUID(), TODAY.plusDays(3).atStartOfDay(), 400),
                new Slice(UUID.randomUUID(), TODAY.minusDays(1).atTime(23, 59), 800));
        assertEquals(300, ChallengeService.secondsWithin(sessions, TODAY, TODAY.plusDays(2)));
        assertEquals(100, ChallengeService.percent(500, 400), "capped at 100");
    }

    @Test
    void earlyBirdAndNightOwlWindows() {
        assertTrue(AchievementService.isEarly(5, 7));
        assertFalse(AchievementService.isEarly(7, 7));
        assertFalse(AchievementService.isEarly(3, 7), "03:00 is still night");
        assertTrue(AchievementService.isLate(23, 22));
        assertTrue(AchievementService.isLate(2, 22));
        assertFalse(AchievementService.isLate(4, 22));
    }
}
