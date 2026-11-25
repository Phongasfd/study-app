package com.namphong.backend.service;

import com.namphong.backend.dto.NotificationPageResponse;
import com.namphong.backend.dto.NotificationResponse;
import com.namphong.backend.entity.Notification;
import com.namphong.backend.entity.NotificationType;
import com.namphong.backend.exception.NotFoundException;
import com.namphong.backend.repository.NotificationRepository;
import com.namphong.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * In-app notifications. Writers call {@link #notify} inside their own transaction, so a
 * notification exists exactly when the change it announces was committed.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    static final int MAX_PAGE_SIZE = 50;
    private static final int TITLE_MAX = 200;
    private static final int MESSAGE_MAX = 1000;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional
    public void notify(UUID userId, NotificationType type, String title, String message, String link) {
        notificationRepository.save(build(userId, type, title, message, link, LocalDateTime.now(clock)));
    }

    /** One batch insert for many recipients (e.g. every member of a group). */
    @Transactional
    public void notifyAll(Collection<UUID> userIds, NotificationType type, String title, String message, String link) {
        if (userIds.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        notificationRepository.saveAll(userIds.stream()
                .distinct()
                .map(userId -> build(userId, type, title, message, link, now))
                .toList());
    }

    private Notification build(UUID userId, NotificationType type, String title, String message, String link,
                               LocalDateTime now) {
        Notification notification = new Notification();
        // a reference, not a SELECT: only the id is needed for the foreign key
        notification.setUser(userRepository.getReferenceById(userId));
        notification.setType(type);
        notification.setTitle(truncate(title, TITLE_MAX));
        notification.setMessage(truncate(message, MESSAGE_MAX));
        notification.setLink(link);
        notification.setCreatedAt(now);
        return notification;
    }

    @Transactional(readOnly = true)
    public NotificationPageResponse list(UUID userId, int page, int size) {
        int safeSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        int safePage = Math.max(0, page);
        Page<Notification> result = notificationRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(safePage, safeSize));
        List<NotificationResponse> items = result.getContent().stream().map(NotificationService::toResponse).toList();
        return new NotificationPageResponse(items, safePage, safeSize, result.getTotalElements(), result.hasNext(),
                notificationRepository.countByUserIdAndReadAtIsNull(userId));
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadAtIsNull(userId);
    }

    /** Idempotent: marking an already read notification again is fine; someone else's is a 404. */
    @Transactional
    public void markRead(UUID notificationId, UUID userId) {
        if (notificationRepository.markRead(notificationId, userId, LocalDateTime.now(clock)) == 0
                && !notificationRepository.existsOwned(notificationId, userId)) {
            throw new NotFoundException("Notification not found");
        }
    }

    @Transactional
    public int markAllRead(UUID userId) {
        return notificationRepository.markAllRead(userId, LocalDateTime.now(clock));
    }

    @Transactional
    public void delete(UUID notificationId, UUID userId) {
        if (notificationRepository.deleteOwned(notificationId, userId) == 0) {
            throw new NotFoundException("Notification not found");
        }
    }

    /** Retention: read notifications older than {@code age} are deleted (see NotificationCleanupJob). */
    @Transactional
    public int purgeReadOlderThan(Duration age) {
        return notificationRepository.deleteReadBefore(LocalDateTime.now(clock).minus(age));
    }

    static NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getMessage(), n.getLink(),
                n.getReadAt() != null, n.getCreatedAt());
    }

    static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max - 1) + "…";
    }
}
