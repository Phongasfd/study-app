package com.namphong.backend.service;

import com.namphong.backend.dto.GroupRankingRequest;
import com.namphong.backend.dto.GroupRankingResponse;
import com.namphong.backend.entity.GroupRanking;
import com.namphong.backend.entity.StudyGroup;
import com.namphong.backend.entity.UserEntity;
import com.namphong.backend.entity.GroupMember;
import com.namphong.backend.entity.SessionStatus;
import com.namphong.backend.entity.StudySession;
import com.namphong.backend.repository.GroupMemberRepository;
import com.namphong.backend.repository.GroupRankingRepository;
import com.namphong.backend.repository.GroupRepository;
import com.namphong.backend.repository.StudySessionRepository;
import com.namphong.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupRankingService {
    private final GroupRankingRepository groupRankingRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final StudySessionRepository studySessionRepository;

    @Transactional(readOnly = true)
    public List<GroupRankingResponse> findGroupRankingById(UUID groupId) {
        List<GroupMember> members = groupMemberRepository.findAllByGroupId(groupId);
        List<GroupRankingResponse> groupRankingResponses = new ArrayList<>();

        for (GroupMember member : members) {
            UserEntity user = member.getUser();

            // Get database total duration (completed study sessions)
            Optional<GroupRanking> rankingOpt = groupRankingRepository.findByGroupIdAndUserId(groupId, user.getId());
            int dbDuration = rankingOpt.map(GroupRanking::getTotalDuration).orElse(0);

            // Get active session if any
            Optional<StudySession> activeSessionOpt = studySessionRepository.findFirstByUserIdAndStatusOrderByStartTimeDesc(user.getId(), SessionStatus.RUNNING);

            int activeDuration = 0;
            boolean isStudying = false;
            LocalDateTime activeSessionStartTime = null;

            if (activeSessionOpt.isPresent()) {
                StudySession activeSession = activeSessionOpt.get();
                isStudying = true;
                activeSessionStartTime = activeSession.getStartTime();
                if (activeSessionStartTime != null) {
                    activeDuration = (int) java.time.Duration.between(activeSessionStartTime, java.time.LocalDateTime.now(java.time.ZoneOffset.UTC)).getSeconds();
                }
            }

            int totalDuration = dbDuration + activeDuration;

            // Calculate daily completed duration for the timer base
            LocalDateTime startOfDay = java.time.LocalDate.now().atStartOfDay();
            List<StudySession> todaySessions = studySessionRepository.findByUserAndStartTimeBetween(user, startOfDay, LocalDateTime.now().plusDays(1));
            int dailyDbDuration = todaySessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.COMPLETED)
                .mapToInt(s -> s.getDurationSeconds() != null ? s.getDurationSeconds() : 0)
                .sum();

            groupRankingResponses.add(
                    GroupRankingResponse.builder()
                            .id(rankingOpt.map(GroupRanking::getId).orElse(null))
                            // method reference "::": rankingOpt.map(groupRanking -> groupRanking.getId()).orElse(null)
                            .groupId(groupId)
                            .userId(user.getId())
                            .username(user.getUsername())
                            .totalDuration(totalDuration)
                            .baseDuration(dailyDbDuration)
                            .isStudying(isStudying)
                            .activeSessionStartTime(activeSessionStartTime)
                            .rank(0) // Will set after sorting
                            .build()
            );
        }

        // Sort by total duration descending
        groupRankingResponses.sort((r1, r2) -> r2.getTotalDuration().compareTo(r1.getTotalDuration()));

        // Assign ranks (handling ties)
        int rank = 1;
        for (int i = 0; i < groupRankingResponses.size(); i++) {
            GroupRankingResponse current = groupRankingResponses.get(i);
            if (i > 0 && !current.getTotalDuration().equals(groupRankingResponses.get(i - 1).getTotalDuration())) {
                rank = i + 1;
            }
            current.setRank(rank);
        }

        return groupRankingResponses;
    }

    public GroupRankingResponse createGroupRanking(GroupRankingRequest request) {
        StudyGroup group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new RuntimeException("Group not found"));
        UserEntity user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<GroupRanking> existingRanking = groupRankingRepository.findByGroupIdAndUserId(request.getGroupId(), request.getUserId());
        if (existingRanking.isPresent()) {
            throw new RuntimeException("Group ranking already exists for this user in this group");
        }

        GroupRanking groupRanking = GroupRanking.builder()
                .group(group)
                .user(user)
                .totalDuration(0)
                .build();

        groupRanking = groupRankingRepository.save(groupRanking);

        return GroupRankingResponse.builder()
                .id(groupRanking.getId())
                .groupId(group.getId())
                .userId(user.getId())
                .username(user.getUsername())
                .totalDuration(groupRanking.getTotalDuration())
                .baseDuration(0) // New rankings have 0 daily time
                .rank(0)
                .build();
    }
}
