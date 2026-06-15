package com.namphong.backend.service;

import com.namphong.backend.dto.UserLoginDTO;
import com.namphong.backend.entity.UserEntity;
import com.namphong.backend.repository.AccountRepository;
import com.namphong.backend.repository.UserRepository;
import com.namphong.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserRepository userRepository;
    private AccountRepository accountRepository;
    private BCryptPasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        accountRepository = mock(AccountRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        jwtService = mock(JwtService.class);
        userService = new UserService(userRepository, accountRepository, passwordEncoder, jwtService);
    }

    @Test
    void authenticate_success() {
        UserEntity user = UserEntity.builder().id(UUID.randomUUID()).email("a@b.com").password(passwordEncoder.encode("secret")).username("u").build();
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));

        UserLoginDTO dto = new UserLoginDTO();
        dto.setEmail("a@b.com");
        dto.setPassword("secret");

        Optional<UserEntity> res = userService.authenticate(dto);
        assertTrue(res.isPresent());
        assertEquals(user.getEmail(), res.get().getEmail());
    }

    @Test
    void authenticate_invalidPassword() {
        UserEntity user = UserEntity.builder().id(UUID.randomUUID()).email("a@b.com").password(passwordEncoder.encode("secret")).username("u").build();
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));

        UserLoginDTO dto = new UserLoginDTO();
        dto.setEmail("a@b.com");
        dto.setPassword("wrong");

        Optional<UserEntity> res = userService.authenticate(dto);
        assertTrue(res.isEmpty());
    }

    @Test
    void login_returnsTokenWhenPasswordMatches() {
        UserEntity user = UserEntity.builder().id(UUID.randomUUID()).email("a@b.com").password(passwordEncoder.encode("secret")).username("u").build();
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user.getId())).thenReturn("token-123");

        UserLoginDTO dto = new UserLoginDTO();
        dto.setEmail("a@b.com");
        dto.setPassword("secret");

        Optional<String> maybe = userService.login(dto);
        assertTrue(maybe.isPresent());
        assertEquals("token-123", maybe.get());
        verify(jwtService, times(1)).generateToken(user.getId());
    }
}
