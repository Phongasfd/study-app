package com.namphong.backend.controller;

import com.namphong.backend.dto.UserLoginDTO;
import com.namphong.backend.dto.UserRequestDTO;
import com.namphong.backend.dto.UserResponseDTO;
import com.namphong.backend.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // ResponseEntity: Object đại diện cho HTTP response
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserLoginDTO userLoginDTO,
                              HttpServletResponse response) {
        return userService.login(userLoginDTO).map(token -> {
            Cookie cookie = new Cookie("access_token", token);
            cookie.setHttpOnly(true);
            cookie.setSecure(false);
            cookie.setPath("/");
            cookie.setMaxAge(15 * 60);
            response.addCookie(cookie);
            return ResponseEntity.ok().build(); // build is used when no body
        })
                .orElse(ResponseEntity.status(401).body("Invalid Credentials"));
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(@RequestBody UserRequestDTO userRequestDTO){
        UserResponseDTO user = userService.register(userRequestDTO);
        return ResponseEntity.status(201).body(user);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response){
        Cookie cookie = new Cookie("access_token", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
        return ResponseEntity.ok("Log out"); // ok return 200, body is Log out
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getUser(HttpServletRequest request){
        if(request.getUserPrincipal() == null) return ResponseEntity.status(401).build();
        UUID userId = UUID.fromString(request.getUserPrincipal().getName());
        return userService.findById(userId).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());

//       .map(ResponseEntity::ok) = map(user -> ResponseEntity.ok(user)) (method reference)
    }

    
}
