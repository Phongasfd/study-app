package com.namphong.backend.controller;

import com.namphong.backend.dto.SubjectRequest;
import com.namphong.backend.dto.SubjectResponse;
import com.namphong.backend.entity.Subject;
import com.namphong.backend.entity.UserEntity;
import com.namphong.backend.service.SubjectService;
import com.namphong.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/subject")
public class SubjectController {
    private final SubjectService subjectService;
    private final UserService userService;

    // Create a subject
    @PostMapping("/")
    public ResponseEntity<SubjectResponse> createSubject(@RequestBody SubjectRequest subject,
                                                         HttpServletRequest request)
    {
        UUID userId = UUID.fromString(request.getUserPrincipal().getName());
        UserEntity user = userService.getUserEntityById(userId);

        Subject created = subjectService.createSubject(subject, user);

        SubjectResponse response = new SubjectResponse(
                created.getId(),
                created.getName(),
                user.getId()
        );
        return ResponseEntity.ok(response);
    }

    // Get subjects by user
    @GetMapping("/")
    public ResponseEntity<List<Subject>> getSubjects(HttpServletRequest request) {
        UUID userId = UUID.fromString(request.getUserPrincipal().getName());

        List<Subject> subjects = subjectService.getSubjectsByUser(userId);
        return ResponseEntity.ok(subjects);
    }

    // Delete a subject
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSubject(@PathVariable UUID id, HttpServletRequest request) {
        UUID userId = UUID.fromString(request.getUserPrincipal().getName());
        
        subjectService.getSubjectById(id).ifPresent(subject -> {
            if (subject.getUser().getId().equals(userId)) {
                subjectService.removeSubject(subject);
            }
        });
        
        return ResponseEntity.noContent().build();
    }
}
