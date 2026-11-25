package com.namphong.backend.repository;

import com.namphong.backend.entity.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AchievementRepository extends JpaRepository<Achievement, String> {
    List<Achievement> findAllByOrderBySortOrderAsc();
}
