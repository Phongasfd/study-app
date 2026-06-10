package com.namphong.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
@Builder
public class GroupRankingResponse {
    private UUID id;
    private UUID groupId;
    private UUID userId;
    private String username;
    private Integer totalDuration;
    private Integer rank;

}
