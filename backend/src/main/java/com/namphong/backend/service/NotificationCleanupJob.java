package com.namphong.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Deletes read notifications after 30 days so the table doesn't grow forever. Unread ones
 * are kept. With several instances each one runs it; the DELETE is idempotent, so that is
 * harmless (a lock such as ShedLock would make it run once).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class NotificationCleanupJob {

    static final Duration RETENTION = Duration.ofDays(30);

    private final NotificationService notificationService;

    public NotificationCleanupJob(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "${app.notifications.cleanup-cron:0 30 3 * * *}")
    public void purgeOldReadNotifications() {
        try {
            int deleted = notificationService.purgeReadOlderThan(RETENTION);
            if (deleted > 0) {
                log.info("Deleted {} read notifications older than {} days", deleted, RETENTION.toDays());
            }
        } catch (Exception e) {
            log.warn("Notification cleanup failed: {}", e.getMessage());
        }
    }
}
