package com.namphong.backend.controller;

import com.namphong.backend.dto.StudyGroupRequest;
import com.namphong.backend.dto.StudyGroupResponse;
import com.namphong.backend.entity.StudyGroup;
import com.namphong.backend.entity.UserEntity;
import com.namphong.backend.service.GroupService;
import com.namphong.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/group")
public class GroupController {
    private final GroupService groupService;
    private final UserService userService;


    // Create a new study group
    @PostMapping("/")
    public ResponseEntity<StudyGroupResponse> createGroup(@RequestBody StudyGroupRequest groupRequest,
                                                         HttpServletRequest request) {
        if (request.getUserPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }
        UUID userId = UUID.fromString(request.getUserPrincipal().getName());
        UserEntity owner = userService.getUserEntityById(userId);

        StudyGroup created = groupService.createGroup(groupRequest, owner);

        StudyGroupResponse response = new StudyGroupResponse(
                created.getId(),
                created.getName(),
                owner.getUsername(),
                created.getMaxMembers()
        );

        return ResponseEntity.ok(response);
    }

    // Delete a study group
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteGroup(@PathVariable UUID id, HttpServletRequest request) {
        if (request.getUserPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }
        UUID userId = UUID.fromString(request.getUserPrincipal().getName());
        groupService.deleteGroup(id, userId);
        return ResponseEntity.noContent().build();
    }   

    // Update group name
    @PatchMapping("/{id}/name")
    public ResponseEntity<StudyGroupResponse> updateGroupName(@PathVariable UUID id,
                                                               @RequestBody StudyGroupRequest groupRequest,
                                                               HttpServletRequest request) {
        if (request.getUserPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }
        UUID userId = UUID.fromString(request.getUserPrincipal().getName());
        StudyGroup updated = groupService.updateGroupName(id, groupRequest.getName(), userId);

        StudyGroupResponse response = new StudyGroupResponse(
                updated.getId(),
                updated.getName(),
                updated.getOwner().getUsername(),
                updated.getMaxMembers()
        );

        return ResponseEntity.ok(response);
    }

    // Get groups joined by the user 
    @GetMapping("/joined")
    public ResponseEntity<List<StudyGroupResponse>> getJoinedGroups(HttpServletRequest request) {
        if (request.getUserPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }
        UUID userId = UUID.fromString(request.getUserPrincipal().getName());
        List<StudyGroupResponse> joinedGroups = groupService.getJoinedGroups(userId)
                .stream()
                .map(group -> new StudyGroupResponse(
                        group.getId(),
                        group.getName(),
                        group.getOwner().getUsername(),
                        group.getMaxMembers()
                ))
                .toList();

        return ResponseEntity.ok(joinedGroups);
    }
}
