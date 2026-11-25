package com.namphong.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_achievements",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_achievements_user_code",
                columnNames = {"user_id", "achievement_code"}))
@Getter
@Setter
@NoArgsConstructor
public class UserAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "achievement_code", nullable = false)
    private Achievement achievement;

    @Column(nullable = false)
    private LocalDateTime unlockedAt;
}
