package com.namphong.backend.controller;

import com.namphong.backend.dto.UserLoginDTO;
import com.namphong.backend.dto.UserRequestDTO;
import com.namphong.backend.dto.UserResponseDTO;
import com.namphong.backend.service.UserService;
import com.namphong.backend.service.RefreshTokenService;
import com.namphong.backend.security.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class UserController {
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;

    public UserController(UserService userService, RefreshTokenService refreshTokenService, JwtService jwtService) {
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
    }

    // ResponseEntity: Object đại diện cho HTTP response
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserLoginDTO userLoginDTO,
                                   HttpServletResponse response) {
        return userService.authenticate(userLoginDTO).map(user -> {
            // create access token
            String access = jwtService.generateToken(user.getId());
            Cookie accessCookie = new Cookie("access_token", access);
            accessCookie.setHttpOnly(true);
            accessCookie.setSecure(true);
            accessCookie.setPath("/");
            accessCookie.setMaxAge(15 * 60);
            response.addCookie(accessCookie);

            // create refresh token (rotate)
            String refresh = refreshTokenService.createRefreshTokenFor(user, 30); // 30 days
            Cookie refreshCookie = new Cookie("refresh_token", refresh);
            refreshCookie.setHttpOnly(true);
            refreshCookie.setSecure(true);
            refreshCookie.setPath("/api/auth");
            refreshCookie.setMaxAge(30 * 24 * 60 * 60);
            response.addCookie(refreshCookie);

            return ResponseEntity.ok().build();
        })
        .orElse(ResponseEntity.status(401).body("Invalid Credentials"));
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(@Valid @RequestBody UserRequestDTO userRequestDTO){
        UserResponseDTO user = userService.register(userRequestDTO);
        return ResponseEntity.status(201).body(user);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response){
        Cookie a = new Cookie("access_token", null);
        a.setMaxAge(0);
        a.setPath("/");
        response.addCookie(a);

        Cookie r = new Cookie("refresh_token", null);
        r.setMaxAge(0);
        r.setPath("/api/auth");
        response.addCookie(r);

        return ResponseEntity.ok("Logged out");
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getUser(HttpServletRequest request){
        if(request.getUserPrincipal() == null) return ResponseEntity.status(401).build();
        UUID userId = UUID.fromString(request.getUserPrincipal().getName());
        return userService.findUserById(userId).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());

    // .map(ResponseEntity::ok) = map(user -> ResponseEntity.ok(user)) (method reference)
    }

    // method for getting new access token using refresh token (rotate) 
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response){
        String refreshCookie = null;
        if(request.getCookies() != null){
            for (Cookie c : request.getCookies()){
                if("refresh_token".equals(c.getName())){ refreshCookie = c.getValue(); break; }
            }
        }

        if(refreshCookie == null) return ResponseEntity.status(401).body("Missing refresh token");

        try{
            var result = refreshTokenService.rotateRefreshToken(refreshCookie);

            // generate new access token
            String newAccess = jwtService.generateToken(result.user.getId());
            Cookie accessCookie = new Cookie("access_token", newAccess);
            accessCookie.setHttpOnly(true);
            accessCookie.setSecure(true);
            accessCookie.setPath("/");
            accessCookie.setMaxAge(15 * 60);
            response.addCookie(accessCookie);

            // set new refresh token cookie (result contains refresh token string)
            Cookie refresh = new Cookie("refresh_token", result.refreshToken);
            refresh.setHttpOnly(true);
            refresh.setSecure(true);
            refresh.setPath("/api/auth");
            refresh.setMaxAge(30 * 24 * 60 * 60);
            response.addCookie(refresh);

            return ResponseEntity.ok().build();
        } catch(Exception e){
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    
}
