package com.namphong.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "group_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "user_id"}))
@Getter
@Setter
public class GroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne // many groupmembers belong to one group
    @JoinColumn(name = "group_id")
    private StudyGroup group;

    @ManyToOne // many groupmembers can belong to one user
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private UserEntity user;

    private LocalDateTime joinedAt;
}