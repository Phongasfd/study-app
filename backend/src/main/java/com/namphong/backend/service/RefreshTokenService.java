package com.namphong.backend.service;

import com.namphong.backend.entity.RefreshToken;
import com.namphong.backend.entity.UserEntity;
import com.namphong.backend.exception.BadRequestException;
import com.namphong.backend.repository.RefreshTokenRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom(); // 

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, BCryptPasswordEncoder passwordEncoder) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // token format: {uuid}:{randomBase64}
    // create new refresh token when user login 
    public String createRefreshTokenFor(UserEntity user, int daysValid) {
        byte[] random = new byte[48];
        secureRandom.nextBytes(random);
        String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(random);

        UUID id = UUID.randomUUID();

        RefreshToken token = RefreshToken.builder()
                .id(id)
                .user(user)
                .tokenHash(passwordEncoder.encode(secret))
                .expiresAt(LocalDateTime.now().plusDays(daysValid))
                .revoked(false)
                .build();

        refreshTokenRepository.save(token);

        return id.toString() + ":" + secret;
    }

    // logout all sessions of user 
    public void revokeAllTokensFor(UserEntity user) {
        List<RefreshToken> tokens = refreshTokenRepository.findByUser(user);
        tokens.forEach(t -> {
            t.setRevoked(true);
        });
        refreshTokenRepository.saveAll(tokens);
    }

    // to replace old refresh token with new refresh
    public RefreshResult rotateRefreshToken(String presentedToken) {
        if (presentedToken == null || !presentedToken.contains(":")) throw new BadRequestException("Invalid refresh token");
        String[] parts = presentedToken.split(":", 2);
        UUID id = UUID.fromString(parts[0]);
        String secret = parts[1];

        Optional<RefreshToken> opt = refreshTokenRepository.findById(id);
        if(opt.isEmpty()) throw new BadRequestException("Refresh token not found");

        RefreshToken current = opt.get();

        // if token already revoked -> possible reuse attack
        if(current.isRevoked()){
            revokeAllTokensFor(current.getUser());
            throw new BadRequestException("Refresh token reuse detected; all sessions revoked");
        }

        // check expiry
        if(current.getExpiresAt() != null && current.getExpiresAt().isBefore(LocalDateTime.now())){
            current.setRevoked(true);
            refreshTokenRepository.save(current);
            throw new BadRequestException("Refresh token expired");
        }

        // verify presented secret
        if(!passwordEncoder.matches(secret, current.getTokenHash())){
            // treat as reuse or tamper
            revokeAllTokensFor(current.getUser());
            throw new BadRequestException("Invalid refresh token");
        }

        // rotate: create new token, mark current revoked and link
        String newToken = createRefreshTokenFor(current.getUser(), 30);
        // parse new id
        UUID newId = UUID.fromString(newToken.split(":",2)[0]);
        current.setRevoked(true);
        current.setReplacedBy(newId);
        refreshTokenRepository.save(current);

        return new RefreshResult(current.getUser(), newToken);
    }


    // static ở đây có nghĩa là không dính object RefreshTokenService, là class độc lập 
    // thuộc về class, không thuộc về object 
    public static class RefreshResult {
        public final UserEntity user;
        public final String refreshToken;

        public RefreshResult(UserEntity user, String refreshToken) {
            this.user = user;
            this.refreshToken = refreshToken;
        }
    }
}
