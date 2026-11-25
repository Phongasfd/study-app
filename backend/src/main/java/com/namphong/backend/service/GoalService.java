package com.namphong.backend.service;

import com.namphong.backend.dto.GoalProgressResponse;
import com.namphong.backend.dto.GoalRequest;
import com.namphong.backend.entity.GoalPeriod;
import com.namphong.backend.entity.StudyGoal;
import com.namphong.backend.entity.Subject;
import com.namphong.backend.exception.BadRequestException;
import com.namphong.backend.exception.NotFoundException;
import com.namphong.backend.repository.SessionSubjectRepository;
import com.namphong.backend.repository.StudyGoalRepository;
import com.namphong.backend.repository.StudySessionRepository;
import com.namphong.backend.repository.StudySessionRepository.SessionSlice;
import com.namphong.backend.repository.SubjectRepository;
import com.namphong.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Study goals and their progress. Progress is computed from completed sessions on read
 * (3 queries for any number of goals), never stored, so it can't drift from the sessions.
 */
@Service
@RequiredArgsConstructor
public class GoalService {

    private final StudyGoalRepository goalRepository;
    private final StudySessionRepository studySessionRepository;
    private final SessionSubjectRepository sessionSubjectRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<GoalProgressResponse> listActive(UUID userId) {
        return withProgress(userId, goalRepository.findActiveWithSubject(userId));
    }

    /** The overall daily goal (the stats page ring), if the user set one. */
    @Transactional(readOnly = true)
    public Optional<GoalProgressResponse> dailyOverall(UUID userId) {
        return goalRepository.findActiveOverall(userId, GoalPeriod.DAILY)
                .map(goal -> withProgress(userId, List.of(goal)).getFirst());
    }

    /**
     * Sets the goal for a scope (period, optional subject), replacing the active one.
     * The old row is deactivated, not deleted. Two concurrent requests for the same scope
     * can't leave two active goals: the partial unique index rejects the second (409).
     */
    @Transactional
    public GoalProgressResponse create(UUID userId, GoalRequest request) {
        validateTarget(request.period(), request.targetMinutes());

        Subject subject = null;
        if (request.subjectId() != null) {
            subject = ownedSubject(request.subjectId(), userId);
            goalRepository.deactivateForSubject(userId, subject.getId(), request.period());
        } else {
            goalRepository.deactivateOverall(userId, request.period());
        }

        StudyGoal goal = new StudyGoal();
        goal.setUser(userRepository.getReferenceById(userId));
        goal.setSubject(subject);
        goal.setPeriod(request.period());
        goal.setTargetSeconds(request.targetMinutes() * 60);
        goal.setActive(true);
        goal.setCreatedAt(LocalDateTime.now(clock));
        StudyGoal saved = goalRepository.saveAndFlush(goal);

        return withProgress(userId, List.of(saved)).getFirst();
    }

    @Transactional
    public GoalProgressResponse updateTarget(UUID userId, UUID goalId, int targetMinutes) {
        StudyGoal goal = activeOwnedGoal(goalId, userId);
        validateTarget(goal.getPeriod(), targetMinutes);
        goal.setTargetSeconds(targetMinutes * 60);
        return withProgress(userId, List.of(goal)).getFirst();
    }

    @Transactional
    public void deactivate(UUID userId, UUID goalId) {
        activeOwnedGoal(goalId, userId).setActive(false);
    }

    // ---------------------------------------------------------------------------------

    private StudyGoal activeOwnedGoal(UUID goalId, UUID userId) {
        return goalRepository.findOwned(goalId, userId)
                .filter(StudyGoal::isActive)
                .orElseThrow(() -> new NotFoundException("Goal not found"));
    }

    private Subject ownedSubject(UUID subjectId, UUID userId) {
        return subjectRepository.findById(subjectId)
                .filter(s -> s.getUser().getId().equals(userId))
                .orElseThrow(() -> new NotFoundException("Subject not found"));
    }

    private static void validateTarget(GoalPeriod period, Integer targetMinutes) {
        if (targetMinutes == null || targetMinutes < 1 || targetMinutes > period.maxMinutes()) {
            throw new BadRequestException("A " + period.name().toLowerCase() + " goal must be between 1 and "
                    + period.maxMinutes() + " minutes");
        }
    }

    /** Loads the sessions of the widest period once, then buckets them per goal in memory. */
    private List<GoalProgressResponse> withProgress(UUID userId, List<StudyGoal> goals) {
        if (goals.isEmpty()) {
            return List.of();
        }
        LocalDate today = LocalDate.now(clock);
        LocalDate from = goals.stream().map(g -> g.getPeriod().start(today)).min(Comparator.naturalOrder()).orElseThrow();
        LocalDate to = goals.stream().map(g -> g.getPeriod().endExclusive(today)).max(Comparator.naturalOrder()).orElseThrow();

        List<SessionSlice> sessions = studySessionRepository.findCompletedSlices(
                userId, from.atStartOfDay(), to.atStartOfDay());

        Map<UUID, List<UUID>> subjectsBySession = Map.of();
        boolean anySubjectGoal = goals.stream().anyMatch(g -> g.getSubject() != null);
        if (anySubjectGoal && !sessions.isEmpty()) {
            subjectsBySession = sessionSubjectRepository
                    .findLinksBySessionIds(sessions.stream().map(SessionSlice::getId).toList())
                    .stream()
                    .collect(Collectors.groupingBy(SessionSubjectRepository.SessionSubjectLink::getSessionId,
                            Collectors.mapping(SessionSubjectRepository.SessionSubjectLink::getSubjectId,
                                    Collectors.toList())));
        }

        List<GoalProgressResponse> result = new ArrayList<>(goals.size());
        for (StudyGoal goal : goals) {
            LocalDate start = goal.getPeriod().start(today);
            LocalDate end = goal.getPeriod().endExclusive(today);
            UUID subjectId = goal.getSubject() != null ? goal.getSubject().getId() : null;
            long progress = progressSeconds(sessions, subjectsBySession, subjectId,
                    start.atStartOfDay(), end.atStartOfDay());
            result.add(toResponse(goal, progress, start, end.minusDays(1)));
        }
        return result;
    }

    /**
     * Seconds studied in [from, to). For a subject goal, a session tagged with several subjects
     * counts for each of them in equal shares: the same rule as the subject statistics.
     */
    static long progressSeconds(List<SessionSlice> sessions, Map<UUID, List<UUID>> subjectsBySession,
                                UUID subjectId, LocalDateTime from, LocalDateTime to) {
        long total = 0;
        for (SessionSlice session : sessions) {
            LocalDateTime start = session.getStartTime();
            int duration = session.getDurationSeconds() != null ? session.getDurationSeconds() : 0;
            if (start == null || duration <= 0 || start.isBefore(from) || !start.isBefore(to)) {
                continue;
            }
            if (subjectId == null) {
                total += duration;
            } else {
                List<UUID> subjects = subjectsBySession.getOrDefault(session.getId(), List.of());
                if (subjects.contains(subjectId)) {
                    total += duration / subjects.size();
                }
            }
        }
        return total;
    }

    private static GoalProgressResponse toResponse(StudyGoal goal, long progress, LocalDate start, LocalDate endInclusive) {
        int target = goal.getTargetSeconds();
        int percent = (int) Math.min(100, progress * 100 / Math.max(1, target));
        return new GoalProgressResponse(
                goal.getId(),
                goal.getPeriod(),
                goal.getSubject() != null ? goal.getSubject().getId() : null,
                goal.getSubject() != null ? goal.getSubject().getName() : null,
                target,
                progress,
                percent,
                progress >= target,
                start,
                endInclusive);
    }
}
