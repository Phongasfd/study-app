package com.namphong.backend.repository;

import com.namphong.backend.entity.GroupRanking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupRankingRepository extends JpaRepository<GroupRanking, UUID> {
    List<GroupRanking> findByGroupIdOrderByTotalDurationDesc(UUID groupId);
    Optional<GroupRanking> findByGroupIdAndUserId(UUID groupId, UUID userId);

    void deleteByStudyGroupId(UUID groupId);
}
