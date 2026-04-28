package com.namphong.backend.repository;

import com.namphong.backend.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {
    Optional<GroupMember> findByGroupIdAndUserId(UUID groupId, UUID userId);
    void deleteByGroupIdAndUserId(UUID groupId, UUID userId);
    long countByGroupId(UUID groupId);
    List<GroupMember> findAllByUserId(UUID userId);
    List<GroupMember> findAllByGroupId(UUID groupId);
}
