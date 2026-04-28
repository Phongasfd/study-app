package com.namphong.backend.controller;

import com.namphong.backend.dto.StudySessionRequest;
import com.namphong.backend.dto.StudySessionResponse;
import com.namphong.backend.entity.StudySession;
import com.namphong.backend.entity.UserEntity;
import com.namphong.backend.service.StudySessionService;
import com.namphong.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.namphong.backend.dto.StudySessionUpdateEndTimeRequest;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/study-session")
public class StudySessionController {
    private final StudySessionService studySessionService;
    private final UserService userService;

    @PostMapping("/")
    public ResponseEntity<StudySessionResponse> createStudySession(@RequestBody StudySessionRequest studySession,
                                                           HttpServletRequest request) {
        if (request.getUserPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }
        UUID userId = UUID.fromString(request.getUserPrincipal().getName());
        UserEntity user = userService.getUserEntityById(userId);
        StudySession session = studySessionService.createStudySession(studySession, user);
        StudySessionResponse studySessionResponse = new StudySessionResponse(
                session.getId(),
                user.getId(),
                session.getStartTime(),
                session.getEndTime(),
                session.getStatus()
        );
        return ResponseEntity.ok(studySessionResponse);
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<StudySessionResponse> completeStudySession(@PathVariable UUID id,
                                                                  @RequestBody StudySessionUpdateEndTimeRequest updateRequest,
                                                                  HttpServletRequest request) {
        if (request.getUserPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }
        
        StudySession session = studySessionService.completeStudySession(id, updateRequest.getEndTime());
        StudySessionResponse studySessionResponse = new StudySessionResponse(
                session.getId(),
                session.getUser().getId(),
                session.getStartTime(),
                session.getEndTime(),
                session.getStatus()
        );
        return ResponseEntity.ok(studySessionResponse);
    }

}
