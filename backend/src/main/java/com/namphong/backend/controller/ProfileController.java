package com.namphong.backend.controller;

import com.namphong.backend.dto.AchievementResponse;
import com.namphong.backend.dto.ProfileSummaryResponse;
import com.namphong.backend.service.AchievementService;
import com.namphong.backend.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/profile")
public class ProfileController {
    private final ProfileService profileService;
    private final AchievementService achievementService;

    @GetMapping("/summary")
    public ProfileSummaryResponse summary(Principal principal) {
        return profileService.summary(CurrentUser.id(principal));
    }

    /** The whole catalog, with what this user unlocked and when. */
    @GetMapping("/achievements")
    public List<AchievementResponse> achievements(Principal principal) {
        return achievementService.listForUser(CurrentUser.id(principal));
    }
}
