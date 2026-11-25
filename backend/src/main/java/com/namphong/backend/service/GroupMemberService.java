package com.namphong.backend.service;

import com.namphong.backend.entity.GroupMember;
import com.namphong.backend.entity.NotificationType;
import com.namphong.backend.entity.StudyGroup;
import com.namphong.backend.entity.UserEntity;
import com.namphong.backend.repository.GroupMemberRepository;
import com.namphong.backend.repository.GroupRepository;
import com.namphong.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.namphong.backend.exception.BadRequestException;
import com.namphong.backend.exception.NotFoundException;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupMemberService {
    private final GroupMemberRepository groupMemberRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher events;

    public boolean isMember(UUID groupId, UUID userId) {
        return groupMemberRepository.existsByGroupIdAndUserId(groupId, userId);
    }

    // Chat, ranking entries, ... are only for people inside the group
    public void requireMember(UUID groupId, UUID userId) {
        if (!isMember(groupId, userId)) {
            throw new AccessDeniedException("You are not a member of this group");
        }
    }

    /**
     * Adds a member if the group has room.
     *
     * "Count members, compare with max_members, insert" let concurrent joins all see the last
     * free seat and all get in. The group row is now locked (SELECT ... FOR UPDATE) first, so
     * joins of one group run one after another and the count is always current.
     */
    @Transactional
    public GroupMember addMember(StudyGroup group, UserEntity user) {
        StudyGroup locked = groupRepository.findByIdForUpdate(group.getId())
                .orElseThrow(() -> new NotFoundException("Group not found"));

        // Check if already a member
        if (groupMemberRepository.findByGroupIdAndUserId(locked.getId(), user.getId()).isPresent()) {
            throw new BadRequestException("User is already a member of this group");
        }

        // Check if group is full
        long currentMembers = groupMemberRepository.countByGroupId(locked.getId());
        if (locked.getMaxMembers() != null && currentMembers >= locked.getMaxMembers()) {
            throw new BadRequestException("Group is full");
        }
        
        GroupMember member = new GroupMember();
        member.setGroup(locked);
        member.setUser(user);
        member.setJoinedAt(LocalDateTime.now());
        GroupMember saved = groupMemberRepository.save(member);

        events.publishEvent(new GroupJoinedEvent(user.getId(), locked.getId()));
        return saved;
    }

    /** The owner adds someone directly (search + invite); the added user is notified. */
    @Transactional
    public GroupMember addMemberAsOwner(UUID groupId, UUID userId, UUID requesterUserId) {
        StudyGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found"));
        if (!group.isOwnedBy(requesterUserId)) {
            throw new AccessDeniedException("Only the owner can add members");
        }
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        GroupMember member = addMember(group, user);
        notificationService.notify(userId, NotificationType.GROUP_MEMBER_ADDED,
                "You were added to " + group.getName(), group.getName(), "/groups/" + groupId);
        return member;
    }

    @Transactional
    public void removeMember(UUID groupId, UUID memberUserId, UUID requesterUserId) {
        StudyGroup group = groupRepository.findById(groupId)
            .orElseThrow(() -> new NotFoundException("Group not found"));

        // Only owner or the member themselves can remove the membership
        boolean isOwner = group.getOwner().getId().equals(requesterUserId);
        boolean isSelf = memberUserId.equals(requesterUserId);

        if (!isOwner && !isSelf) {
            throw new AccessDeniedException("You don't have permission to remove this member");
        }

        // Owner cannot leave, they must delete the group
        if (group.getOwner().getId().equals(memberUserId)) {
             throw new BadRequestException("The owner cannot leave the group. Delete the group instead.");
        }

        groupMemberRepository.deleteByGroupIdAndUserId(groupId, memberUserId);

        if (!isSelf) {
            notificationService.notify(memberUserId, NotificationType.GROUP_MEMBER_REMOVED,
                    "You were removed from " + group.getName(), group.getName(), "/groups");
        }
    }
}
