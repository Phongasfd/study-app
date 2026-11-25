package com.namphong.backend.dto;

import com.namphong.backend.entity.GroupVisibility;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class StudyGroupRequest {
    private String name;
    private Integer maxMembers;
    /** optional, defaults to PUBLIC */
    private GroupVisibility visibility;
    private String description;
}
