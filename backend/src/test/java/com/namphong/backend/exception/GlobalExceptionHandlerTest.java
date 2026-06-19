package com.namphong.backend.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.namphong.backend.config.RateLimiterProperties;
import com.namphong.backend.controller.UserController;
import com.namphong.backend.dto.UserRequestDTO;
import com.namphong.backend.service.RefreshTokenService;
import com.namphong.backend.service.UserService;
import com.namphong.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
// import static cho phép bạn import trực tiếp các thành viên static (method hoặc biến) của một 
// class, để có thể sử dụng chúng mà không cần ghi tên class phía trước.

@WebMvcTest(controllers = UserController.class)
@Import(ObjectMapper.class) // Spring tạo Bean ObjectMapper
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private RateLimiterProperties rateLimiterProperties;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @MockitoBean
    private JwtService jwtService;

    // test cases: khi gửi payload không hợp lệ (ví dụ: thiếu email, password quá ngắn) thì sẽ trả về lỗi validation và message "Validation failed"
    @Test
    void validationError_returnsApiError() throws Exception {
        // send invalid register payload (missing email and short password)
        UserRequestDTO dto = new UserRequestDTO();
        dto.setUsername("");
        dto.setEmail("not-an-email");
        dto.setPassword("123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors").isArray());
    }
}
