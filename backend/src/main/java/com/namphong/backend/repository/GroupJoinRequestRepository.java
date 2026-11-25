package com.namphong.backend.repository;

import com.namphong.backend.entity.GroupJoinRequest;
import com.namphong.backend.entity.JoinRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupJoinRequestRepository extends JpaRepository<GroupJoinRequest, UUID> {

    /** Partial unique index from V4: one PENDING request per (group, user). */
    String ONE_PENDING_INDEX = "uq_group_join_requests_pending";

    Optional<GroupJoinRequest> findByGroupIdAndUserIdAndStatus(UUID groupId, UUID userId, JoinRequestStatus status);

    @Query("select r from GroupJoinRequest r join fetch r.user where r.group.id = :groupId and r.status = :status " +
           "order by r.createdAt")
    List<GroupJoinRequest> findWithUserByGroup(@Param("groupId") UUID groupId,
                                               @Param("status") JoinRequestStatus status);

    @Query("select r from GroupJoinRequest r join fetch r.group where r.user.id = :userId and r.status = :status " +
           "order by r.createdAt desc")
    List<GroupJoinRequest> findWithGroupByUser(@Param("userId") UUID userId,
                                               @Param("status") JoinRequestStatus status);

    @Query("select r from GroupJoinRequest r join fetch r.user join fetch r.group where r.id = :id")
    Optional<GroupJoinRequest> findWithUserAndGroup(@Param("id") UUID id);

    /**
     * Moves a PENDING request to its final state in one statement. Returns 1 for the request
     * that decided it, 0 if it was already decided (double click on "approve", two owners'
     * tabs, approve racing a cancel): only the winner adds the member and notifies.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update GroupJoinRequest r set r.status = :to, r.decidedAt = :now, r.decidedBy = :decidedBy " +
           "where r.id = :id and r.group.id = :groupId and r.status = :pending")
    int decide(@Param("id") UUID id,
               @Param("groupId") UUID groupId,
               @Param("to") JoinRequestStatus to,
               @Param("now") LocalDateTime now,
               @Param("decidedBy") UUID decidedBy,
               @Param("pending") JoinRequestStatus pending);

    default int decide(UUID id, UUID groupId, JoinRequestStatus to, LocalDateTime now, UUID decidedBy) {
        return decide(id, groupId, to, now, decidedBy, JoinRequestStatus.PENDING);
    }
}
