package com.namphong.backend.repository;

import com.namphong.backend.entity.StudyGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GroupRepository extends JpaRepository<StudyGroup, UUID> {
}
