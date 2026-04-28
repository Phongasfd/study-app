package com.namphong.backend.repository;

import com.namphong.backend.entity.StudyGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface GroupRepository extends JpaRepository<StudyGroup, UUID> {
    @Query(value = "SELECT * FROM study_groups ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<StudyGroup> findRandomGroups(int limit);

    List<StudyGroup> findByNameContainingIgnoreCase(String name);
}
