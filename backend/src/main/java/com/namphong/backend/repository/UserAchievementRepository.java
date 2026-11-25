package com.namphong.backend.repository;

import com.namphong.backend.entity.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, UUID> {

    interface Unlocked {
        String getCode();
        LocalDateTime getUnlockedAt();
    }

    @Query("select ua.achievement.code as code, ua.unlockedAt as unlockedAt from UserAchievement ua " +
           "where ua.user.id = :userId")
    List<Unlocked> findUnlocked(@Param("userId") UUID userId);

    long countByUserId(UUID userId);

    /**
     * Unlocks an achievement once. Returns 1 for the call that unlocked it and 0 if it was
     * already unlocked, including by a concurrent evaluation: the unique (user_id,
     * achievement_code) constraint decides, so the "unlocked" notification is sent once.
     */
    @Modifying(flushAutomatically = true)
    @Query(value = "insert into user_achievements (id, user_id, achievement_code, unlocked_at) " +
                   "values (gen_random_uuid(), :userId, :code, :at) " +
                   "on conflict (user_id, achievement_code) do nothing",
           nativeQuery = true)
    int unlock(@Param("userId") UUID userId, @Param("code") String code, @Param("at") LocalDateTime at);
}
