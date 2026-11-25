package com.namphong.backend.repository;

import com.namphong.backend.entity.StudyTask;
import com.namphong.backend.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudyTaskRepository extends JpaRepository<StudyTask, UUID>, JpaSpecificationExecutor<StudyTask> {

    @Query("select t from StudyTask t left join fetch t.subject where t.id = :id and t.user.id = :userId")
    Optional<StudyTask> findOwned(@Param("id") UUID id, @Param("userId") UUID userId);

    long countByUserIdAndStatus(UUID userId, TaskStatus status);

    interface StatusCount {
        TaskStatus getStatus();
        Long getTotal();
    }

    @Query("select t.status as status, count(t) as total from StudyTask t where t.user.id = :userId group by t.status")
    List<StatusCount> countByStatus(@Param("userId") UUID userId);

    @Query("select count(t) from StudyTask t where t.user.id = :userId and t.status <> :done and t.dueDate < :today")
    long countOverdue(@Param("userId") UUID userId, @Param("done") TaskStatus done, @Param("today") LocalDate today);

    @Query("select count(t) from StudyTask t where t.user.id = :userId and t.status <> :done and t.dueDate = :today")
    long countDueOn(@Param("userId") UUID userId, @Param("done") TaskStatus done, @Param("today") LocalDate today);
}
