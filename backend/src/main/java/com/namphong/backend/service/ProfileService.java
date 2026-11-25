package com.namphong.backend.service;

import com.namphong.backend.dto.ProfileSummaryResponse;
import com.namphong.backend.entity.SessionStatus;
import com.namphong.backend.entity.TaskStatus;
import com.namphong.backend.entity.UserEntity;
import com.namphong.backend.exception.NotFoundException;
import com.namphong.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final StudySessionRepository studySessionRepository;
    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final StudyTaskRepository taskRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ProfileSummaryResponse summary(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        StreakCalculator.Streak streak = StreakCalculator.compute(
                studySessionRepository.findStudyDays(userId), LocalDate.now(clock));
        return new ProfileSummaryResponse(
                userId,
                user.getUsername(),
                studySessionRepository.sumDurationByUserId(userId, SessionStatus.COMPLETED),
                studySessionRepository.countByUserIdAndStatus(userId, SessionStatus.COMPLETED),
                streak.current(),
                streak.longest(),
                userAchievementRepository.countByUserId(userId),
                achievementRepository.count(),
                taskRepository.countByUserIdAndStatus(userId, TaskStatus.DONE),
                groupMemberRepository.countByUserId(userId));
    }
}
