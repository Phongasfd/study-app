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
import com.namphong.backend.exception.NotFoundException;
import com.namphong.backend.exception.BadRequestException;
import org.springframework.security.access.AccessDeniedException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        // 4 queries for the whole group, however many members it has. The old loop ran
        // 3 queries per member (+1 for the EAGER user), and this runs on every timer start/stop broadcast.
        List<GroupMember> members = groupMemberRepository.findAllWithUserByGroupId(groupId);
        List<GroupRankingResponse> groupRankingResponses = new ArrayList<>();
        if (members.isEmpty()) {
            return groupRankingResponses;
        }
        List<UUID> userIds = members.stream().map(m -> m.getUser().getId()).toList();

        // Database total duration (completed study sessions) per user
        Map<UUID, GroupRankingRepository.RankingTotal> rankings = new HashMap<>();
        for (GroupRankingRepository.RankingTotal row : groupRankingRepository.findTotalsByGroupId(groupId)) {
            rankings.put(row.getUserId(), row);
        }

        // Running session start time per user
        Map<UUID, LocalDateTime> runningSince = studySessionRepository.findLatestRunningStartTimes(userIds);

        // Today's completed duration per user (timer base)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        Map<UUID, Long> todaySeconds = new HashMap<>();
        for (StudySessionRepository.UserDuration row : studySessionRepository.sumDurationByUserIds(
                userIds, SessionStatus.COMPLETED, startOfDay, now.plusDays(1))) {
            todaySeconds.put(row.getUserId(), row.getTotalSeconds());
        }

        for (GroupMember member : members) {
            UserEntity user = member.getUser();

            GroupRankingRepository.RankingTotal ranking = rankings.get(user.getId());
            int dbDuration = ranking != null && ranking.getTotalDuration() != null ? ranking.getTotalDuration() : 0;

            int activeDuration = 0;
            boolean isStudying = false;
            LocalDateTime activeSessionStartTime = null;

            // Sessions running longer than the cap are treated as abandoned (not studying);
            // StudySessionService closes them with the 12h cap on the next start/complete.
            LocalDateTime startTime = runningSince.get(user.getId());
            if (startTime != null && !StudySessionService.isAbandoned(startTime, now)) {
                isStudying = true;
                activeSessionStartTime = startTime;
                activeDuration = (int) Duration.between(startTime, now).getSeconds();
            }

            int totalDuration = dbDuration + activeDuration;
            int dailyDbDuration = todaySeconds.getOrDefault(user.getId(), 0L).intValue();

            groupRankingResponses.add(
                    GroupRankingResponse.builder()
                            .id(ranking != null ? ranking.getId() : null)
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

    public GroupRankingResponse createGroupRanking(GroupRankingRequest request, UUID requesterId) {
        // Users may only open their own ranking entry, and only in a group they belong to
        if (!requesterId.equals(request.getUserId())) {
            throw new AccessDeniedException("You can only create your own ranking entry");
        }
        if (groupMemberRepository.findByGroupIdAndUserId(request.getGroupId(), requesterId).isEmpty()) {
            throw new AccessDeniedException("You are not a member of this group");
        }

        StudyGroup group = groupRepository.findById(request.getGroupId())
            .orElseThrow(() -> new NotFoundException("Group not found"));
        UserEntity user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new NotFoundException("User not found"));

        Optional<GroupRanking> existingRanking = groupRankingRepository.findByGroupIdAndUserId(request.getGroupId(), request.getUserId());
        if (existingRanking.isPresent()) {
            throw new BadRequestException("Group ranking already exists for this user in this group");
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
