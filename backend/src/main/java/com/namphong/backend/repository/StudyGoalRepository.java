package com.namphong.backend.repository;

import com.namphong.backend.entity.GoalPeriod;
import com.namphong.backend.entity.StudyGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudyGoalRepository extends JpaRepository<StudyGoal, UUID> {

    /** Partial unique indexes from V4: one active goal per scope. */
    String ONE_ACTIVE_OVERALL_INDEX = "uq_study_goals_active_overall";
    String ONE_ACTIVE_SUBJECT_INDEX = "uq_study_goals_active_subject";

    @Query("select g from StudyGoal g left join fetch g.subject where g.user.id = :userId and g.active = true " +
           "order by g.createdAt")
    List<StudyGoal> findActiveWithSubject(@Param("userId") UUID userId);

    @Query("select g from StudyGoal g left join fetch g.subject where g.id = :id and g.user.id = :userId")
    Optional<StudyGoal> findOwned(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select g from StudyGoal g where g.user.id = :userId and g.period = :period and g.subject is null " +
           "and g.active = true")
    Optional<StudyGoal> findActiveOverall(@Param("userId") UUID userId, @Param("period") GoalPeriod period);

    // Two methods instead of one with "(:subjectId is null or ...)": PostgreSQL can't infer
    // the type of a null-only parameter and rejects the statement.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update StudyGoal g set g.active = false where g.user.id = :userId and g.period = :period " +
           "and g.subject is null and g.active = true")
    int deactivateOverall(@Param("userId") UUID userId, @Param("period") GoalPeriod period);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update StudyGoal g set g.active = false where g.user.id = :userId and g.period = :period " +
           "and g.subject.id = :subjectId and g.active = true")
    int deactivateForSubject(@Param("userId") UUID userId, @Param("subjectId") UUID subjectId,
                             @Param("period") GoalPeriod period);
}
