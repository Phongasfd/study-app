package com.namphong.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "accounts",
        uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "providerAccountId"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false) // user_id column, take ownership of relationship
    private UserEntity user;

    @Column(nullable = false)
    private String provider;
    // "google", "facebook", "local"

    @Column(nullable = false)
    private String providerAccountId;
    // googleId, facebookId, or username/email

}