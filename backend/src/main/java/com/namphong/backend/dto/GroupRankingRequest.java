package com.namphong.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class GroupRankingRequest {
    private UUID groupId;
    private UUID userId;
}
