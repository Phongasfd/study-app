package com.namphong.backend.repository;

import com.namphong.backend.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {
    Optional<GroupMember> findByGroupIdAndUserId(UUID groupId, UUID userId);
    void deleteByGroupIdAndUserId(UUID groupId, UUID userId);
    void deleteAllByGroupId(UUID groupId); 
    long countByGroupId(UUID groupId);
    List<GroupMember> findAllByUserId(UUID userId);
    List<GroupMember> findAllByGroupId(UUID groupId);

    // One query for members + their users/group/owner. The derived findAllByGroupId loads the
    // EAGER @ManyToOne user with an extra SELECT per member (N+1).
    @Query("select gm from GroupMember gm join fetch gm.user join fetch gm.group g left join fetch g.owner where g.id = :groupId")
    List<GroupMember> findAllWithUserByGroupId(@Param("groupId") UUID groupId);
}
