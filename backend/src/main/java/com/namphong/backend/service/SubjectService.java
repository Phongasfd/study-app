package com.namphong.backend.service;

import com.namphong.backend.dto.SubjectRequest;
import com.namphong.backend.entity.Subject;
import com.namphong.backend.entity.UserEntity;
import com.namphong.backend.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubjectService {
    private final SubjectRepository subjectRepository;

    public Subject createSubject(SubjectRequest subject, UserEntity user) {
        Subject newSubject = new Subject();
        newSubject.setName(subject.getName());
        newSubject.setUser(user);

        return subjectRepository.save(newSubject);
    }

    public List<Subject> getSubjectsByUser(UUID userId) {
        return subjectRepository.findByUserId(userId);
    }

    public Optional<Subject> getSubjectById(UUID id) {
        return subjectRepository.findById(id);
    }

    public void removeSubject(Subject subject) {
        subjectRepository.delete(subject);
    }
}
