package com.namphong.backend.controller;

import com.namphong.backend.dto.StudyGroupDetailResponse;
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
                .stream() // to use map for list 
                .map(group -> new StudyGroupResponse(
                        group.getId(),
                        group.getName(),
                        group.getOwner().getUsername(),
                        group.getMaxMembers()
                ))
                .toList();

        return ResponseEntity.ok(joinedGroups);
    }

    // Get 4 random study groups for trending
    @GetMapping("/random")
    public ResponseEntity<List<StudyGroupResponse>> getRandomGroups() {
        List<StudyGroupResponse> randomGroups = groupService.getRandomGroups(4)
                .stream()
                .map(group -> new StudyGroupResponse(
                        group.getId(),
                        group.getName(),
                        group.getOwner().getUsername(),
                        group.getMaxMembers()
                ))
                .toList();
        return ResponseEntity.ok(randomGroups);
    }

    // Get group details by id
    @GetMapping("/{id}")
    public ResponseEntity<StudyGroupDetailResponse> getGroupDetails(@PathVariable UUID id) {
        StudyGroup group = groupService.getGroupById(id);
        
        List<StudyGroupDetailResponse.MemberResponse> members = groupService.getGroupMembers(id)
                .stream()
                .map(member -> new StudyGroupDetailResponse.MemberResponse(
                        member.getUser().getId(),
                        member.getUser().getUsername(),
                        member.getUser().getEmail(),
                        group.getOwner().getId().equals(member.getUser().getId()) ? "Owner" : "Member",
                        "online", // Placeholder for status
                        null // Placeholder for avatar
                ))
                .toList();

        StudyGroupDetailResponse response = new StudyGroupDetailResponse(
                group.getId(),
                group.getName(),
                group.getOwner().getUsername(),
                group.getMaxMembers(),
                members
        );
        return ResponseEntity.ok(response);
    }

    // Search groups by name
    @GetMapping("/search")
    public ResponseEntity<List<StudyGroupResponse>> searchGroups(@RequestParam String name) {
        List<StudyGroupResponse> results = groupService.searchGroups(name)
                .stream()
                .map(group -> new StudyGroupResponse(
                        group.getId(),
                        group.getName(),
                        group.getOwner().getUsername(),
                        group.getMaxMembers()
                ))
                .toList();
        return ResponseEntity.ok(results);
    }
}
