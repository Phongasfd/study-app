package com.namphong.backend.controller;

import com.namphong.backend.dto.JoinRequestResponse;
import com.namphong.backend.service.GroupJoinService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/** Join requests of PRIVATE groups. Owners decide; requesters can see and withdraw their own. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/group")
public class JoinRequestController {
    private final GroupJoinService groupJoinService;

    @GetMapping("/{groupId}/join-requests")
    public List<JoinRequestResponse> pending(@PathVariable UUID groupId, Principal principal) {
        return groupJoinService.pendingForGroup(groupId, CurrentUser.id(principal));
    }

    @PostMapping("/{groupId}/join-requests/{requestId}/approve")
    public ResponseEntity<Void> approve(@PathVariable UUID groupId, @PathVariable UUID requestId, Principal principal) {
        groupJoinService.approve(groupId, requestId, CurrentUser.id(principal));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{groupId}/join-requests/{requestId}/reject")
    public ResponseEntity<Void> reject(@PathVariable UUID groupId, @PathVariable UUID requestId, Principal principal) {
        groupJoinService.reject(groupId, requestId, CurrentUser.id(principal));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{groupId}/join-requests/mine")
    public ResponseEntity<Void> cancelMine(@PathVariable UUID groupId, Principal principal) {
        groupJoinService.cancel(groupId, CurrentUser.id(principal));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/join-requests/mine")
    public List<JoinRequestResponse> mine(Principal principal) {
        return groupJoinService.pendingForUser(CurrentUser.id(principal));
    }
}
