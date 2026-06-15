package com.namphong.backend.service;

import com.namphong.backend.entity.RefreshToken;
import com.namphong.backend.entity.UserEntity;
import com.namphong.backend.exception.BadRequestException;
import com.namphong.backend.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RefreshTokenServiceTest {
    private RefreshTokenRepository repo;
    private BCryptPasswordEncoder encoder;
    private RefreshTokenService service;

    // before each test case, do this 
    @BeforeEach
    void setUp() {
        repo = mock(RefreshTokenRepository.class);
        encoder = new BCryptPasswordEncoder();
        service = new RefreshTokenService(repo, encoder);
    }

    // test case: rotate refresh token successfully
    @Test
    void rotateRefreshToken_success() {
        UUID id = UUID.randomUUID();
        String secret = "my-secret-xyz";
        UserEntity user = UserEntity.builder().id(UUID.randomUUID()).email("a@b.com").username("u").build();
        RefreshToken current = RefreshToken.builder()
                .id(id)
                .tokenHash(encoder.encode(secret))
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(5))
                .revoked(false)
                .build();

        when(repo.findById(id)).thenReturn(Optional.of(current));
        when(repo.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        var result = service.rotateRefreshToken(id.toString() + ":" + secret);
        assertNotNull(result);
        assertNotNull(result.refreshToken);
        assertTrue(result.refreshToken.contains(":"));

        // verify that previous token was revoked and saved with replacedBy set
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repo, atLeastOnce()).save(captor.capture());
        List<RefreshToken> saved = captor.getAllValues();
        boolean foundRevoked = saved.stream().anyMatch(t -> t.getId().equals(id) && t.isRevoked() && t.getReplacedBy() != null);
        assertTrue(foundRevoked, "previous token should be revoked and linked to replacement");
    }

    // test cases: rotate refresh token with reuse detection, should revoke all tokens for user
    @Test
    void rotateRefreshToken_reuseDetected_revokesAll() {
        UUID id = UUID.randomUUID();
        String secret = "secret-reuse";
        UserEntity user = UserEntity.builder().id(UUID.randomUUID()).email("a@b.com").username("u").build();
        
            /*
            * Token đã bị revoke nhưng client vẫn gửi lại.
            * Đây là dấu hiệu refresh token bị đánh cắp.
            */
        RefreshToken current = RefreshToken.builder()
                .id(id)
                .tokenHash(encoder.encode(secret))
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(5))
                .revoked(true)
                .build();

        when(repo.findById(id)).thenReturn(Optional.of(current));
        when(repo.findByUser(user)).thenReturn(List.of(current));
        when(repo.saveAll(any())).thenAnswer(i -> i.getArgument(0));
        // Khác với:
        // thenReturn(...)

        // thenAnswer(...) cho phép:
        // Tự quyết định giá trị trả về dựa trên tham số truyền vào.
        // Khi saveAll(...) được gọi với bất kỳ danh sách nào, hãy trả về chính danh sách đó.

        assertThrows(BadRequestException.class, () -> service.rotateRefreshToken(id.toString() + ":" + secret));

        // verify that revokeAllTokensFor called saveAll
        verify(repo, times(1)).saveAll(any());
    }
}
