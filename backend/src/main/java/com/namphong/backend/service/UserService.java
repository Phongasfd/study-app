package com.namphong.backend.service;

import com.namphong.backend.dto.UserLoginDTO;
import com.namphong.backend.dto.UserRequestDTO;
import com.namphong.backend.dto.UserResponseDTO;
import com.namphong.backend.entity.AccountEntity;
import com.namphong.backend.entity.UserEntity;
import com.namphong.backend.repository.AccountRepository;
import com.namphong.backend.repository.UserRepository;
import com.namphong.backend.security.JwtService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
//    Dùng Optional khi:
//    Query DB
//    Tìm kiếm
//    Login / authentication
//    Bất kỳ thứ gì có thể fail mà không phải exception

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository, AccountRepository accountRepository, BCryptPasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public Optional<String> login(UserLoginDTO userLoginDTO) {
        Optional<UserEntity> userOpt = userRepository.findByEmail(userLoginDTO.getEmail());
        if(userOpt.isEmpty()) return Optional.empty();

        UserEntity user = userOpt.get();
        if(passwordEncoder.matches(userLoginDTO.getPassword(), user.getPassword())){
            // Generate JWT
            String token = jwtService.generateToken(user.getId());
            return Optional.of(token);
        }
        return Optional.empty();
    }

    public UserResponseDTO register(UserRequestDTO userRequestDTO) {
        UserEntity user = new UserEntity();
        user.setEmail(userRequestDTO.getEmail());
        user.setPassword(passwordEncoder.encode(userRequestDTO.getPassword()));
        user.setUsername(userRequestDTO.getUsername());

        UserEntity saved = userRepository.save(user);

        // Create local account
        AccountEntity account = new AccountEntity();
        account.setUser(saved);
        account.setProvider("local");
        account.setProviderAccountId(saved.getEmail());
        accountRepository.save(account);
        
        return new UserResponseDTO(saved.getId(), saved.getEmail(), saved.getUsername(), saved.getCreatedAt());
    }

    public Optional<UserResponseDTO> findUserById(UUID userId) {
        return userRepository.findById(userId).map(user -> new UserResponseDTO(user.getId(), user.getEmail(), user.getUsername(), user.getCreatedAt()));
    }

    public UserEntity getUserEntityById(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
    }
}
