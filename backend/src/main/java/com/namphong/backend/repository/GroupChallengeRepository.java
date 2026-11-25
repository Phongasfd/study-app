package com.namphong.backend.repository;

import com.namphong.backend.entity.GroupChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupChallengeRepository extends JpaRepository<GroupChallenge, UUID> {

    @Query("select c from GroupChallenge c join fetch c.createdBy where c.group.id = :groupId " +
           "order by c.endDate desc, c.createdAt desc")
    List<GroupChallenge> findByGroupIdWithCreator(@Param("groupId") UUID groupId);

    @Query("select c from GroupChallenge c join fetch c.createdBy join fetch c.group g join fetch g.owner " +
           "where c.id = :id and g.id = :groupId")
    Optional<GroupChallenge> findInGroup(@Param("id") UUID id, @Param("groupId") UUID groupId);

    /** Challenges running today in any group the user belongs to. */
    @Query("select c from GroupChallenge c where c.startDate <= :today and c.endDate >= :today " +
           "and c.group.id in (select m.group.id from GroupMember m where m.user.id = :userId)")
    List<GroupChallenge> findActiveForUser(@Param("userId") UUID userId, @Param("today") LocalDate today);
}
