package com.namphong.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.namphong.backend.entity.UserEntity;
import com.namphong.backend.dto.UserLoginDTO;
import com.namphong.backend.service.RefreshTokenService;
import com.namphong.backend.service.UserService;
import com.namphong.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false) // remove every filter
// chỉ khởi tạo layer web (controller, filter, validation...)
// không load toàn bộ spring context nên sẽ nhanh hơn @SpringBootTest 
class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;
    // giả lập http request mà không cần chạy server thật

    @Autowired
    private ObjectMapper objectMapper;
    // chuyển object java sang json và ngược lại
    @TestConfiguration
    static class TestConfig {
        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @MockitoBean
    private UserService userService;
    // mock service vì không dùng service thật 

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @MockitoBean
    private JwtService jwtService;

    // test case: khi login thành công thì sẽ set cookie access_token và refresh_token 
    @Test
    void login_setsCookiesOnSuccess() throws Exception {
        UserEntity user = UserEntity.builder().id(UUID.randomUUID()).email("a@b.com").username("u").build();
        when(userService.authenticate(any())).thenReturn(Optional.of(user)); 
        // any(): Bất kể đối tượng nào được truyền vào method này, hãy trả về kết quả đã mock
        when(jwtService.generateToken(user.getId())).thenReturn("access-xyz");
        when(refreshTokenService.createRefreshTokenFor(any(UserEntity.class), anyInt())).thenReturn("r-id:secret");

        UserLoginDTO dto = new UserLoginDTO();
        dto.setEmail("a@b.com");
        dto.setPassword("password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("access_token"))
                .andExpect(cookie().exists("refresh_token"));
    }

    // test case: khi login thất bại thì sẽ trả về 401 Unauthorized
    @Test
    void login_invalidCredentials_returns401() throws Exception {
        when(userService.authenticate(any())).thenReturn(Optional.empty());

        UserLoginDTO dto = new UserLoginDTO();
        dto.setEmail("a@b.com");
        dto.setPassword("wrong");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }
}
