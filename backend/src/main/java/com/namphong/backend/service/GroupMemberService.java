package com.namphong.backend.service;

import com.namphong.backend.entity.GroupMember;
import com.namphong.backend.entity.StudyGroup;
import com.namphong.backend.entity.UserEntity;
import com.namphong.backend.repository.GroupMemberRepository;
import com.namphong.backend.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
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

    @Transactional
    public GroupMember addMember(StudyGroup group, UserEntity user) {
        // Check if already a member
        if (groupMemberRepository.findByGroupIdAndUserId(group.getId(), user.getId()).isPresent()) {
            throw new BadRequestException("User is already a member of this group");
        }

        // Check if group is full
        long currentMembers = groupMemberRepository.countByGroupId(group.getId());
        if (group.getMaxMembers() != null && currentMembers >= group.getMaxMembers()) {
            throw new BadRequestException("Group is full");
        }
        
        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(user);
        member.setJoinedAt(LocalDateTime.now());
        return groupMemberRepository.save(member);
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
    }
}
