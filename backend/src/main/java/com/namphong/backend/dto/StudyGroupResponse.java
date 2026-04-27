package com.namphong.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class StudyGroupResponse {
    private UUID id;
    private String name;
    private String ownerName;
    private Integer maxMembers;
}
