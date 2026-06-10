package com.namphong.backend.controller;

import com.namphong.backend.dto.GroupRankingRequest;
import com.namphong.backend.dto.GroupRankingResponse;
import com.namphong.backend.service.GroupRankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/group-ranking")
public class GroupRankingController {
    private final GroupRankingService groupRankingService;

    @GetMapping("/{groupId}")
    public ResponseEntity<List<GroupRankingResponse>> getGroupRanking(@PathVariable UUID groupId) {
        return ResponseEntity.ok(groupRankingService.findGroupRankingById(groupId));
    }

    @PostMapping
    public ResponseEntity<GroupRankingResponse> createGroupRanking(@RequestBody GroupRankingRequest request) {
        return ResponseEntity.ok(groupRankingService.createGroupRanking(request));
    }
}
