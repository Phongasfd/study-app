package com.namphong.backend.controller;

import com.namphong.backend.dto.ChallengeDetailResponse;
import com.namphong.backend.dto.ChallengeRequest;
import com.namphong.backend.dto.ChallengeResponse;
import com.namphong.backend.service.ChallengeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/group/{groupId}/challenges")
public class ChallengeController {
    private final ChallengeService challengeService;

    @GetMapping
    public List<ChallengeResponse> list(@PathVariable UUID groupId, Principal principal) {
        return challengeService.list(groupId, CurrentUser.id(principal));
    }

    @GetMapping("/{challengeId}")
    public ChallengeDetailResponse detail(@PathVariable UUID groupId, @PathVariable UUID challengeId,
                                          Principal principal) {
        return challengeService.detail(groupId, challengeId, CurrentUser.id(principal));
    }

    @PostMapping
    public ResponseEntity<ChallengeResponse> create(@PathVariable UUID groupId,
                                                    @Valid @RequestBody ChallengeRequest request,
                                                    Principal principal) {
        return ResponseEntity.status(201).body(challengeService.create(groupId, CurrentUser.id(principal), request));
    }

    @DeleteMapping("/{challengeId}")
    public ResponseEntity<Void> delete(@PathVariable UUID groupId, @PathVariable UUID challengeId,
                                       Principal principal) {
        challengeService.delete(groupId, challengeId, CurrentUser.id(principal));
        return ResponseEntity.noContent().build();
    }
}
