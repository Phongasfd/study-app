package com.namphong.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "session_subjects",
        uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "subject_id"}))
@Getter
@Setter
public class SessionSubject {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "session_id")
    private StudySession session;

    @ManyToOne
    @JoinColumn(name = "subject_id")
    private Subject subject;
}