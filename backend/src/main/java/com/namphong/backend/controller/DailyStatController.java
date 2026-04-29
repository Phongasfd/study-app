package com.namphong.backend.controller;

import com.namphong.backend.dto.DailyStatResponse;
import com.namphong.backend.dto.WeeklyAggregateResponse;
import com.namphong.backend.entity.UserEntity;
import com.namphong.backend.service.DailyStatService;
import com.namphong.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stat")
public class DailyStatController {
    private final DailyStatService dailyStatService;
    private final UserService userService;

    @PostMapping("/sync")
    public ResponseEntity<DailyStatResponse> syncDailyStat(HttpServletRequest request) {
        if (request.getUserPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }
        UUID userId = UUID.fromString(request.getUserPrincipal().getName());
        UserEntity user = userService.getUserEntityById(userId);
        DailyStatResponse result = dailyStatService.syncDailyStat(user);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/weekly")
    public ResponseEntity<List<DailyStatResponse>> getWeeklyStats(HttpServletRequest request) {
        if (request.getUserPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }
        UUID userId = UUID.fromString(request.getUserPrincipal().getName());
        UserEntity user = userService.getUserEntityById(userId);
        List<DailyStatResponse> stats = dailyStatService.getWeeklyStats(user);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/monthly")
    public ResponseEntity<List<WeeklyAggregateResponse>> getMonthlyStats(HttpServletRequest request) {
        if (request.getUserPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }
        UUID userId = UUID.fromString(request.getUserPrincipal().getName());
        UserEntity user = userService.getUserEntityById(userId);
        List<WeeklyAggregateResponse> stats = dailyStatService.getMonthlyStats(user);
        return ResponseEntity.ok(stats);
    }
}
