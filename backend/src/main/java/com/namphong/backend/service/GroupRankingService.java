package com.namphong.backend.service;

import com.namphong.backend.dto.GroupRankingRequest;
import com.namphong.backend.dto.GroupRankingResponse;
import com.namphong.backend.entity.GroupRanking;
import com.namphong.backend.entity.StudyGroup;
import com.namphong.backend.entity.UserEntity;
import com.namphong.backend.repository.GroupRankingRepository;
import com.namphong.backend.repository.GroupRepository;
import com.namphong.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    public List<GroupRankingResponse> findGroupRankingById(UUID groupId) {
       List<GroupRanking> rankings = groupRankingRepository.findByGroupIdOrderByTotalDurationDesc(groupId);

       List<GroupRankingResponse> groupRankingResponses = new ArrayList<>();

       int rank = 1;

       for (int i = 0; i < rankings.size(); i++) {
           GroupRanking current = rankings.get(i);

           // handle the same ranking
           if (i > 0 && !current.getTotalDuration().equals(rankings.get(i-1).getTotalDuration())) {
               rank = i + 1;
           }

           groupRankingResponses.add(
                   GroupRankingResponse.builder()
                           .id(current.getId())
                           .groupId(groupId)
                           .userId(current.getUser().getId())
                           .username(current.getUser().getUsername())
                           .totalDuration(current.getTotalDuration())
                           .rank(rank)
                           .build()
           );
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
                .rank(0)
                .build();
    }
}
