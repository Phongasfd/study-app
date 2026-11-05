package com.namphong.backend.websocket;

import com.namphong.backend.entity.GroupMember;
import com.namphong.backend.repository.GroupMemberRepository;
import com.namphong.backend.service.GroupRankingService;
import com.namphong.backend.service.StudySessionChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/**
 * Pushes the new leaderboard to every group of a user whose session started or stopped.
 *
 * Runs AFTER_COMMIT: the board is read from committed data, and a rolled-back start/stop
 * (for example the losing side of two concurrent starts) never reaches anyone's screen.
 * Before, the push happened inside the transaction, so clients could see a ranking
 * that was then rolled back.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RankingBroadcaster {

    private final GroupMemberRepository groupMemberRepository;
    private final GroupRankingService groupRankingService;
    private final GroupEventBus groupEventBus;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onStudySessionChanged(StudySessionChangedEvent event) {
        // Never throw from here: the change is already committed, and an exception in an
        // after-commit callback would still reach the controller and turn the response into a 500
        try {
            for (GroupMember member : groupMemberRepository.findAllByUserId(event.userId())) {
                broadcastRanking(member.getGroup().getId());
            }
        } catch (Exception e) {
            log.error("Failed to broadcast rankings for user {}: {}", event.userId(), e.getMessage());
        }
    }

    // computed once here, then fanned out to every node by the GroupEventBus
    public void broadcastRanking(UUID groupId) {
        try {
            groupEventBus.publish(groupId, "ranking", groupRankingService.findGroupRankingById(groupId));
        } catch (Exception e) {
            log.error("Failed to broadcast ranking for group {}: {}", groupId, e.getMessage());
        }
    }
}
