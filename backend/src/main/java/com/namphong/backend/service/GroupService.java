package com.namphong.backend.service;

import com.namphong.backend.dto.StudyGroupRequest;
import com.namphong.backend.entity.GroupMember;
import com.namphong.backend.entity.StudyGroup;
import com.namphong.backend.entity.UserEntity;
import com.namphong.backend.repository.GroupMemberRepository;
import com.namphong.backend.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.namphong.backend.exception.NotFoundException;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupService {
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;

    @Transactional
    public StudyGroup createGroup(StudyGroupRequest request, UserEntity owner) {
        StudyGroup group = new StudyGroup();
        group.setName(request.getName());
        group.setOwner(owner);
        group.setMaxMembers(request.getMaxMembers());
        group.setCreatedAt(LocalDateTime.now());
        
        StudyGroup savedGroup = groupRepository.save(group);

        // Add owner as the first member
        GroupMember member = new GroupMember();
        member.setGroup(savedGroup);
        member.setUser(owner);
        member.setJoinedAt(LocalDateTime.now());
        groupMemberRepository.save(member);

        return savedGroup;
    }

    @Transactional
    public void deleteGroup(UUID groupId, UUID userId) {
        StudyGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found"));

        if (group.getOwner().getId().equals(userId)) {
            groupMemberRepository.deleteAllByGroupId(groupId);  // Delete all members first
            groupRepository.delete(group);  // Then delete the group
        } else {
            throw new AccessDeniedException("Only the owner can delete the group");
        }
    }

    @Transactional
    public StudyGroup updateGroupName(UUID groupId, String newName, UUID userId) {
        StudyGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found"));

        if (group.getOwner().getId().equals(userId)) {
            group.setName(newName);
            return groupRepository.save(group);
        } else {
            throw new AccessDeniedException("Only the owner can update the group name");
        }
    }

    public List<StudyGroup> getJoinedGroups(UUID userId) {
        return groupMemberRepository.findAllByUserId(userId)
                .stream()
                .map(GroupMember::getGroup)
                // groupMember -> groupMember.getGroup()
                .toList();
    }

    public List<StudyGroup> getRandomGroups(int limit) {
        return groupRepository.findRandomGroups(limit);
    }

    public StudyGroup getGroupById(UUID groupId) {
        return groupRepository.findById(groupId)
            .orElseThrow(() -> new NotFoundException("Group not found"));
    }

    public List<GroupMember> getGroupMembers(UUID groupId) {
        return groupMemberRepository.findAllByGroupId(groupId);
    }

    public List<StudyGroup> searchGroups(String name) {
        return groupRepository.findByNameContainingIgnoreCase(name);
    }
}