package com.namphong.backend.repository;

import com.namphong.backend.entity.GroupRanking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GroupRankingRepository extends JpaRepository<GroupRanking, UUID> {
}
