package com.namphong.backend.repository;

import com.namphong.backend.entity.StudyGroup;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupRepository extends JpaRepository<StudyGroup, UUID> {
    @Query(value = "SELECT * FROM study_groups ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<StudyGroup> findRandomGroups(int limit);

    List<StudyGroup> findByNameContainingIgnoreCase(String name);

    /**
     * SELECT ... FOR UPDATE on the group row. Joins of one group take this lock before
     * counting members, so concurrent joins (or approvals) can't all see "one seat left"
     * and push the group past max_members.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from StudyGroup g where g.id = :id")
    Optional<StudyGroup> findByIdForUpdate(@Param("id") UUID id);
}
