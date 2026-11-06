package com.namphong.backend.websocket;

import com.namphong.backend.repository.GroupMemberRepository;
import com.namphong.backend.service.GroupRankingService;
import com.namphong.backend.service.StudySessionChangedEvent;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Pushes the new leaderboard to every group of a user whose session started or stopped.
 *
 * Runs AFTER_COMMIT: the board is read from committed data, and a rolled-back start/stop
 * (for example the losing side of two concurrent starts) never reaches anyone's screen.
 * Before, the push happened inside the transaction, so clients could see a ranking
 * that was then rolled back.
 *
 * The board is read on one background thread, not in the after-commit callback itself. That
 * callback runs on the request thread while the committed transaction still holds its database
 * connection, so reading there took a second connection per request: about ten simultaneous
 * starts on the default pool of 10 deadlocked the pool until Hikari's 30 s timeout, and every
 * push of that burst was lost. One thread also keeps the pushes of a group in commit order.
 */
@Slf4j
@Component
public class RankingBroadcaster {

    /** Pending pushes beyond this are dropped (logged): the next push carries the full board anyway. */
    static final int MAX_PENDING = 10_000;

    private final GroupMemberRepository groupMemberRepository;
    private final GroupRankingService groupRankingService;
    private final GroupEventBus groupEventBus;
    private final TransactionTemplate readOnly;
    private final ExecutorService worker = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(MAX_PENDING),
            runnable -> {
                Thread thread = new Thread(runnable, "ranking-broadcast");
                thread.setDaemon(true);
                return thread;
            });

    public RankingBroadcaster(GroupMemberRepository groupMemberRepository,
                              GroupRankingService groupRankingService,
                              GroupEventBus groupEventBus,
                              PlatformTransactionManager transactionManager) {
        this.groupMemberRepository = groupMemberRepository;
        this.groupRankingService = groupRankingService;
        this.groupEventBus = groupEventBus;
        this.readOnly = new TransactionTemplate(transactionManager);
        this.readOnly.setReadOnly(true);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStudySessionChanged(StudySessionChangedEvent event) {
        // Never throw from here: the change is already committed, and an exception in an
        // after-commit callback would still reach the controller and turn the response into a 500
        try {
            worker.execute(() -> pushRankings(event.userId()));
        } catch (RejectedExecutionException e) {
            log.warn("Ranking push for user {} dropped: {} pushes already pending or shutting down",
                    event.userId(), MAX_PENDING);
        }
    }

    private void pushRankings(UUID userId) {
        try {
            List<UUID> groupIds = readOnly.execute(status -> groupMemberRepository.findAllByUserId(userId).stream()
                    .map(member -> member.getGroup().getId())
                    .toList());
            for (UUID groupId : groupIds) {
                broadcastRanking(groupId);
            }
        } catch (Exception e) {
            log.error("Failed to broadcast rankings for user {}: {}", userId, e.getMessage());
        }
    }

    // computed once here, then fanned out to every node by the GroupEventBus
    public void broadcastRanking(UUID groupId) {
        try {
            groupEventBus.publish(groupId, "ranking", readOnly.execute(status ->
                    groupRankingService.findGroupRankingById(groupId)));
        } catch (Exception e) {
            log.error("Failed to broadcast ranking for group {}: {}", groupId, e.getMessage());
        }
    }

    @PreDestroy
    void shutdown() {
        worker.shutdown();
    }
}
