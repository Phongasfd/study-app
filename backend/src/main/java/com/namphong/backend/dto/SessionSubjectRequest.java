package com.namphong.backend.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
public class SessionSubjectRequest {
    private UUID sessionId;
    private UUID subjectId;
}
