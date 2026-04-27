package com.namphong.backend.dto;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class StudyGroupRequest {
    private String name;
    private Integer maxMembers;
}
