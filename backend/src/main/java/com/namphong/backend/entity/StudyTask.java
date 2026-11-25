package com.namphong.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "study_tasks")
@Getter
@Setter
@NoArgsConstructor
public class StudyTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @Column(nullable = false)
    private String title;

    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskPriority priority = TaskPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.TODO;

    private LocalDate dueDate;

    private Integer estimatedMinutes;

    /** Set exactly when status is DONE (enforced by a CHECK constraint, V4). */
    private LocalDateTime completedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /** Moves the task to {@code next}; returns true if it just became DONE. */
    public boolean transitionTo(TaskStatus next, LocalDateTime now) {
        boolean becameDone = next == TaskStatus.DONE && status != TaskStatus.DONE;
        status = next;
        if (next == TaskStatus.DONE) {
            if (completedAt == null) {
                completedAt = now;
            }
        } else {
            completedAt = null;
        }
        updatedAt = now;
        return becameDone;
    }

    public boolean isOverdue(LocalDate today) {
        return status != TaskStatus.DONE && dueDate != null && dueDate.isBefore(today);
    }
}
