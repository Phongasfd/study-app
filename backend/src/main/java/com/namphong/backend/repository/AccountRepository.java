package com.namphong.backend.repository;

import com.namphong.backend.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {
    boolean existsByProviderAndProviderAccountId(String google, String googleId);
}
