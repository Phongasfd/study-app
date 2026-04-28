package com.namphong.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class StudyGroupDetailResponse {
    private UUID id;
    private String name;
    private String ownerName;
    private Integer maxMembers;
    private List<MemberResponse> members;

    @Getter
    @AllArgsConstructor
    public static class MemberResponse {
        private UUID id;
        private String username;
        private String email;
        private String role;
        private String status;
        private String avatar;
    }
}
