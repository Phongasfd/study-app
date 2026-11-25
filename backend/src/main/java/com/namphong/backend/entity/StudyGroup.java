package com.namphong.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "study_groups")
@Getter
@Setter
public class StudyGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    @JsonIgnore
    private UserEntity owner;

    private Integer maxMembers;

    private LocalDateTime createdAt;

    // PRIVATE groups are joined through a request the owner approves (V4)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupVisibility visibility = GroupVisibility.PUBLIC;

    private String description;

    public boolean isOwnedBy(java.util.UUID userId) {
        return owner != null && owner.getId().equals(userId);
    }
}