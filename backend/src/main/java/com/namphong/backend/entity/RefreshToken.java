package com.namphong.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    private Long version;

    // hashed token secret part
    @Column(nullable = false)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY) // user chỉ load khi gọi getUser() 
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;

    private boolean revoked = false; // token đã bị thu hồi (revoke) hay chưa 

    private UUID replacedBy; // id of the token that replaced this one
}
