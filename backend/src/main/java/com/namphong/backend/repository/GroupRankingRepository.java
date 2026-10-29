package com.namphong.backend.repository;

import com.namphong.backend.entity.GroupRanking;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
