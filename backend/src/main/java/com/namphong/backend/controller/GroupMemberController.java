package com.namphong.backend.controller;

import com.namphong.backend.entity.GroupMember;
import com.namphong.backend.entity.StudyGroup;
import com.namphong.backend.entity.UserEntity;
import com.namphong.backend.repository.GroupRepository;
import com.namphong.backend.service.GroupMemberService;
import com.namphong.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/group-member")
public class GroupMemberController {
    private final GroupMemberService groupMemberService;
    private final GroupRepository groupRepository;
    private final UserService userService;

    @PostMapping("/join/{groupId}")
    public ResponseEntity<GroupMember> joinGroup(@PathVariable UUID groupId, HttpServletRequest request) {
        if (request.getUserPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }
        UUID userId = UUID.fromString(request.getUserPrincipal().getName());
        UserEntity user = userService.getUserEntityById(userId);
        StudyGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        GroupMember member = groupMemberService.addMember(group, user);
        return ResponseEntity.ok(member);
    }

    @DeleteMapping("/{groupId}/user/{userId}")
    public ResponseEntity<?> removeMember(@PathVariable UUID groupId, @PathVariable UUID userId, HttpServletRequest request) {
        if (request.getUserPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }
        UUID requesterUserId = UUID.fromString(request.getUserPrincipal().getName());
        groupMemberService.removeMember(groupId, userId, requesterUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{groupId}/add/{userId}")
    public ResponseEntity<GroupMember> addMember(@PathVariable UUID groupId, @PathVariable UUID userId, HttpServletRequest request) {
        if (request.getUserPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }
        UUID requesterUserId = UUID.fromString(request.getUserPrincipal().getName());
        StudyGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        if (!group.getOwner().getId().equals(requesterUserId)) {
            return ResponseEntity.status(403).build();
        }

        UserEntity userToAdd = userService.getUserEntityById(userId);
        GroupMember member = groupMemberService.addMember(group, userToAdd);
        return ResponseEntity.ok(member);
    }
}
