package com.namphong.backend.repository;

import com.namphong.backend.entity.RefreshToken;
import com.namphong.backend.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    List<RefreshToken> findByUser(UserEntity user);
    Optional<RefreshToken> findById(UUID id);
}
