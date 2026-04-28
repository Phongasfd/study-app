package com.namphong.backend.controller;

import com.namphong.backend.dto.SessionSubjectRequest;
import com.namphong.backend.dto.SessionSubjectResponse;
import com.namphong.backend.entity.SessionSubject;
import com.namphong.backend.service.SessionSubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/session-subject")
public class SessionSubjectController {
    private final SessionSubjectService sessionSubjectService;

    @PostMapping("/")
    public ResponseEntity<SessionSubjectResponse> addSubjectToSession(@RequestBody SessionSubjectRequest request) {
        SessionSubject sessionSubject = sessionSubjectService.addSubjectToSession(
                request.getSessionId(),
                request.getSubjectId()
        );

        SessionSubjectResponse response = new SessionSubjectResponse(
                sessionSubject.getId(),
                sessionSubject.getSession().getId(),
                sessionSubject.getSubject().getId(),
                sessionSubject.getSubject().getName()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<SessionSubjectResponse>> getSubjectsBySession(@PathVariable UUID sessionId) {
        List<SessionSubject> sessionSubjects = sessionSubjectService.getSubjectsBySession(sessionId);
        
        List<SessionSubjectResponse> response = sessionSubjects.stream()
                .map(ss -> new SessionSubjectResponse(
                        ss.getId(),
                        ss.getSession().getId(),
                        ss.getSubject().getId(),
                        ss.getSubject().getName()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

}
