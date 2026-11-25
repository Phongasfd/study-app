package com.namphong.backend.controller;

import com.namphong.backend.dto.NotificationPageResponse;
import com.namphong.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public NotificationPageResponse list(@RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size,
                                         Principal principal) {
        return notificationService.list(CurrentUser.id(principal), page, size);
    }

    /** Polled by the bell in the top bar; one index-only count. */
    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(Principal principal) {
        return Map.of("count", notificationService.unreadCount(CurrentUser.id(principal)));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable UUID id, Principal principal) {
        notificationService.markRead(id, CurrentUser.id(principal));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public Map<String, Integer> markAllRead(Principal principal) {
        return Map.of("updated", notificationService.markAllRead(CurrentUser.id(principal)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Principal principal) {
        notificationService.delete(id, CurrentUser.id(principal));
        return ResponseEntity.noContent().build();
    }
}
