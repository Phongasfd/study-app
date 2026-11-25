package com.namphong.backend.service;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Runs the achievement evaluation after a session, task or membership change commits.
 *
 * Same shape as RankingBroadcaster, for the same reasons: AFTER_COMMIT so a rolled-back
 * change never unlocks anything, and on one background thread because the after-commit
 * callback still holds the request's database connection (reading there needs a second one
 * per request and can exhaust the pool). The user's request doesn't wait for it either.
 */
@Slf4j
@Component
public class AchievementEvaluator {

    static final int MAX_PENDING = 10_000;

    private final AchievementService achievementService;
    private final ExecutorService worker = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(MAX_PENDING),
            runnable -> {
                Thread thread = new Thread(runnable, "achievement-evaluator");
                thread.setDaemon(true);
                return thread;
            });

    public AchievementEvaluator(AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSessionCompleted(StudySessionCompletedEvent event) {
        submit(event.userId(), new AchievementService.SessionFacts(event.startTime(), event.durationSeconds()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskCompleted(TaskCompletedEvent event) {
        submit(event.userId(), null);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onGroupJoined(GroupJoinedEvent event) {
        submit(event.userId(), null);
    }

    private void submit(UUID userId, AchievementService.SessionFacts facts) {
        // never throw from an after-commit callback: the change is committed already
        try {
            worker.execute(() -> {
                try {
                    achievementService.evaluate(userId, facts);
                } catch (Exception e) {
                    log.error("Achievement evaluation failed for user {}: {}", userId, e.getMessage());
                }
            });
        } catch (RejectedExecutionException e) {
            // the next trigger re-evaluates everything, so nothing is lost for good
            log.warn("Achievement evaluation for user {} dropped: queue full or shutting down", userId);
        }
    }

    @PreDestroy
    void shutdown() {
        worker.shutdown();
    }
}
