package com.namphong.backend.repository;

import com.namphong.backend.entity.UserEntity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(@Email @NotBlank String email);

    List<UserEntity> findByUsernameContainingIgnoreCase(String username);
}
