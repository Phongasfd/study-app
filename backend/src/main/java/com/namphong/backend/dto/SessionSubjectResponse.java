package com.namphong.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SessionSubjectResponse {
    private UUID id;
    private UUID sessionId;
    private UUID subjectId;
    private String subjectName;
}
