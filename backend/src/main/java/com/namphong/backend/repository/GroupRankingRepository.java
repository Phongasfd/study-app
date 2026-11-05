package com.namphong.backend.repository;

import com.namphong.backend.entity.GroupRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupRankingRepository extends JpaRepository<GroupRanking, UUID> {
    List<GroupRanking> findByGroupIdOrderByTotalDurationDesc(UUID groupId);
    Optional<GroupRanking> findByGroupIdAndUserId(UUID groupId, UUID userId);

    void deleteByGroup_Id(UUID studyGroupId);

    // Projection: only the columns the ranking board needs, for the whole group in one query
    interface RankingTotal {
        UUID getUserId();
        UUID getId();
        Integer getTotalDuration();
    }

    @Query("select r.user.id as userId, r.id as id, r.totalDuration as totalDuration from GroupRanking r where r.group.id = :groupId")
    List<RankingTotal> findTotalsByGroupId(@Param("groupId") UUID groupId);

    /**
     * Adds seconds to a member's total in one atomic statement, creating the row on first use.
     *
     * Replaces "find row, total + seconds in Java, save": two transactions doing that at
     * once both read the old total and one increment is lost, and two first-time inserts
     * collide on unique (group_id, user_id). The upsert does the arithmetic inside
     * PostgreSQL under the row lock, so concurrent increments add up.
     */
    @Modifying(flushAutomatically = true)
    @Query(value = "insert into group_rankings (id, group_id, user_id, total_duration) " +
                   "values (gen_random_uuid(), :groupId, :userId, :seconds) " +
                   "on conflict (group_id, user_id) do update " +
                   "set total_duration = coalesce(group_rankings.total_duration, 0) + excluded.total_duration",
           nativeQuery = true)
    int addDuration(@Param("groupId") UUID groupId, @Param("userId") UUID userId, @Param("seconds") int seconds);
}
