package com.namphong.backend.controller;

import com.namphong.backend.dto.JoinGroupRequest;
import com.namphong.backend.dto.JoinResultResponse;
import com.namphong.backend.service.GroupJoinService;
import com.namphong.backend.service.GroupMemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/group-member")
public class GroupMemberController {
    private final GroupMemberService groupMemberService;
    private final GroupJoinService groupJoinService;

    /**
     * PUBLIC group: joins at once ({"status":"JOINED"}).
     * PRIVATE group: files a request for the owner ({"status":"REQUESTED"}); the body may carry a message.
     */
    @PostMapping("/join/{groupId}")
    public ResponseEntity<JoinResultResponse> joinGroup(@PathVariable UUID groupId,
                                                        @Valid @RequestBody(required = false) JoinGroupRequest body,
                                                        HttpServletRequest request) {
        if (request.getUserPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }
        UUID userId = UUID.fromString(request.getUserPrincipal().getName());
        JoinResultResponse result = groupJoinService.join(groupId, userId, body != null ? body.message() : null);
        return ResponseEntity.ok(result);
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
    public ResponseEntity<Map<String, Object>> addMember(@PathVariable UUID groupId, @PathVariable UUID userId,
                                                         HttpServletRequest request) {
        if (request.getUserPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }
        UUID requesterUserId = UUID.fromString(request.getUserPrincipal().getName());
        // owner check, capacity, notification to the added user: GroupMemberService
        groupMemberService.addMemberAsOwner(groupId, userId, requesterUserId);
        return ResponseEntity.ok(Map.of("groupId", groupId, "userId", userId));
    }
}
