package com.namphong.backend.repository;

import com.namphong.backend.entity.DailyStat;
import com.namphong.backend.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DailyStatRepository extends JpaRepository<DailyStat, UUID> {
    List<DailyStat> findByUserAndDateBetween(UserEntity user, LocalDate start, LocalDate end);
    Optional<DailyStat> findByUserAndDate(UserEntity user, LocalDate date);
}
