package com.namphong.backend.controller;

import com.namphong.backend.dto.GoalProgressResponse;
import com.namphong.backend.dto.GoalRequest;
import com.namphong.backend.dto.GoalUpdateRequest;
import com.namphong.backend.service.GoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/goals")
public class GoalController {
    private final GoalService goalService;

    @GetMapping
    public List<GoalProgressResponse> list(Principal principal) {
        return goalService.listActive(CurrentUser.id(principal));
    }

    /** The overall daily goal; 204 if none is set. */
    @GetMapping("/daily")
    public ResponseEntity<GoalProgressResponse> daily(Principal principal) {
        return goalService.dailyOverall(CurrentUser.id(principal))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping
    public ResponseEntity<GoalProgressResponse> create(@Valid @RequestBody GoalRequest request, Principal principal) {
        return ResponseEntity.status(201).body(goalService.create(CurrentUser.id(principal), request));
    }

    @PutMapping("/{id}")
    public GoalProgressResponse update(@PathVariable UUID id, @Valid @RequestBody GoalUpdateRequest request,
                                       Principal principal) {
        return goalService.updateTarget(CurrentUser.id(principal), id, request.targetMinutes());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Principal principal) {
        goalService.deactivate(CurrentUser.id(principal), id);
        return ResponseEntity.noContent().build();
    }
}
