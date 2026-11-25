package com.namphong.backend.dto;

import com.namphong.backend.entity.GroupVisibility;
import com.namphong.backend.entity.StudyGroup;
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
    private GroupVisibility visibility;
    private String description;

    public static StudyGroupResponse from(StudyGroup group) {
        return new StudyGroupResponse(
                group.getId(),
                group.getName(),
                group.getOwner() != null ? group.getOwner().getUsername() : null,
                group.getMaxMembers(),
                group.getVisibility() != null ? group.getVisibility() : GroupVisibility.PUBLIC,
                group.getDescription());
    }
}
