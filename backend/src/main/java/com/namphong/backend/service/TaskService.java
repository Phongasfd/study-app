package com.namphong.backend.service;

import com.namphong.backend.dto.TaskRequest;
import com.namphong.backend.dto.TaskResponse;
import com.namphong.backend.dto.TaskSummaryResponse;
import com.namphong.backend.entity.*;
import com.namphong.backend.exception.BadRequestException;
import com.namphong.backend.exception.NotFoundException;
import com.namphong.backend.repository.StudyTaskRepository;
import com.namphong.backend.repository.SubjectRepository;
import com.namphong.backend.repository.UserRepository;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** The personal to-do list. Every lookup is scoped to the owner: another user's task is a 404. */
@Service
@RequiredArgsConstructor
public class TaskService {

    /** Due-date filters accepted by {@link #list}. */
    public enum DueFilter { OVERDUE, TODAY, WEEK, NONE }

    private final StudyTaskRepository taskRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    /**
     * Open tasks first; among them overdue, then by due date (undated last), then by priority.
     * Done tasks follow, most recently completed first.
     */
    static Comparator<StudyTask> displayOrder(LocalDate today) {
        return Comparator
                .comparing((StudyTask t) -> t.getStatus() == TaskStatus.DONE)
                .thenComparing(t -> !t.isOverdue(today))
                .thenComparing(StudyTask::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(t -> -t.getPriority().ordinal())
                .thenComparing(StudyTask::getCompletedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(StudyTask::getCreatedAt, Comparator.reverseOrder());
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> list(UUID userId, TaskStatus status, UUID subjectId, String due) {
        LocalDate today = LocalDate.now(clock);
        DueFilter dueFilter = parseDue(due);

        Specification<StudyTask> spec = (root, query, cb) -> {
            // fetch the subject with the task (one query), except in a count query
            if (query != null && !Long.class.equals(query.getResultType())) {
                root.fetch("subject", JoinType.LEFT);
            }
            return cb.equal(root.get("user").get("id"), userId);
        };
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (subjectId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("subject").get("id"), subjectId));
        }
        spec = switch (dueFilter) {
            case OVERDUE -> spec.and((root, query, cb) -> cb.and(
                    cb.notEqual(root.get("status"), TaskStatus.DONE),
                    cb.lessThan(root.get("dueDate"), today)));
            case TODAY -> spec.and((root, query, cb) -> cb.equal(root.get("dueDate"), today));
            case WEEK -> spec.and((root, query, cb) -> cb.between(root.get("dueDate"), today, today.plusDays(6)));
            case NONE -> spec;
        };

        return taskRepository.findAll(spec).stream()
                .sorted(displayOrder(today))
                .map(task -> toResponse(task, today))
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskSummaryResponse summary(UUID userId) {
        LocalDate today = LocalDate.now(clock);
        long todo = 0, inProgress = 0, done = 0;
        for (StudyTaskRepository.StatusCount row : taskRepository.countByStatus(userId)) {
            switch (row.getStatus()) {
                case TODO -> todo = row.getTotal();
                case IN_PROGRESS -> inProgress = row.getTotal();
                case DONE -> done = row.getTotal();
            }
        }
        return new TaskSummaryResponse(todo, inProgress, done,
                taskRepository.countOverdue(userId, TaskStatus.DONE, today),
                taskRepository.countDueOn(userId, TaskStatus.DONE, today));
    }

    @Transactional
    public TaskResponse create(UUID userId, TaskRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);
        StudyTask task = new StudyTask();
        task.setUser(userRepository.getReferenceById(userId));
        task.setCreatedAt(now);
        task.setStatus(TaskStatus.TODO);
        apply(task, request, userId);
        boolean done = task.transitionTo(request.status() != null ? request.status() : TaskStatus.TODO, now);
        StudyTask saved = taskRepository.save(task);
        if (done) {
            events.publishEvent(new TaskCompletedEvent(userId, saved.getId()));
        }
        return toResponse(saved, now.toLocalDate());
    }

    @Transactional
    public TaskResponse update(UUID userId, UUID taskId, TaskRequest request) {
        StudyTask task = owned(taskId, userId);
        LocalDateTime now = LocalDateTime.now(clock);
        apply(task, request, userId);
        boolean done = task.transitionTo(request.status() != null ? request.status() : task.getStatus(), now);
        if (done) {
            events.publishEvent(new TaskCompletedEvent(userId, taskId));
        }
        return toResponse(task, now.toLocalDate());
    }

    @Transactional
    public TaskResponse changeStatus(UUID userId, UUID taskId, TaskStatus status) {
        StudyTask task = owned(taskId, userId);
        LocalDateTime now = LocalDateTime.now(clock);
        if (task.transitionTo(status, now)) {
            events.publishEvent(new TaskCompletedEvent(userId, taskId));
        }
        return toResponse(task, now.toLocalDate());
    }

    @Transactional
    public void delete(UUID userId, UUID taskId) {
        taskRepository.delete(owned(taskId, userId));
    }

    // ---------------------------------------------------------------------------------

    private StudyTask owned(UUID taskId, UUID userId) {
        return taskRepository.findOwned(taskId, userId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
    }

    /** Copies the editable fields; status is handled by StudyTask.transitionTo. */
    private void apply(StudyTask task, TaskRequest request, UUID userId) {
        task.setTitle(request.title().trim());
        task.setNotes(blankToNull(request.notes()));
        task.setPriority(request.priority() != null ? request.priority() : TaskPriority.MEDIUM);
        task.setDueDate(request.dueDate());
        task.setEstimatedMinutes(request.estimatedMinutes());
        if (request.subjectId() == null) {
            task.setSubject(null);
        } else {
            task.setSubject(subjectRepository.findById(request.subjectId())
                    .filter(s -> s.getUser().getId().equals(userId))
                    .orElseThrow(() -> new NotFoundException("Subject not found")));
        }
    }

    static DueFilter parseDue(String due) {
        if (due == null || due.isBlank()) {
            return DueFilter.NONE;
        }
        try {
            return DueFilter.valueOf(due.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("due must be one of overdue, today, week");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    static TaskResponse toResponse(StudyTask task, LocalDate today) {
        Subject subject = task.getSubject();
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getNotes(),
                subject != null ? subject.getId() : null,
                subject != null ? subject.getName() : null,
                task.getPriority(),
                task.getStatus(),
                task.getDueDate(),
                task.getEstimatedMinutes(),
                task.getCompletedAt(),
                task.getCreatedAt(),
                task.isOverdue(today));
    }
}
