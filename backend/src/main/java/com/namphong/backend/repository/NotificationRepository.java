package com.namphong.backend.repository;

import com.namphong.backend.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    // served by the partial index idx_notifications_user_unread
    long countByUserIdAndReadAtIsNull(UUID userId);

    // "and n.user.id = :userId": a user can only touch their own notifications;
    // someone else's id simply matches 0 rows (reported as 404, no existence leak)
    @Modifying
    @Query("update Notification n set n.readAt = :now where n.id = :id and n.user.id = :userId and n.readAt is null")
    int markRead(@Param("id") UUID id, @Param("userId") UUID userId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("update Notification n set n.readAt = :now where n.user.id = :userId and n.readAt is null")
    int markAllRead(@Param("userId") UUID userId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("delete from Notification n where n.id = :id and n.user.id = :userId")
    int deleteOwned(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select count(n) > 0 from Notification n where n.id = :id and n.user.id = :userId")
    boolean existsOwned(@Param("id") UUID id, @Param("userId") UUID userId);

    @Modifying
    @Query("delete from Notification n where n.readAt is not null and n.createdAt < :before")
    int deleteReadBefore(@Param("before") LocalDateTime before);
}
