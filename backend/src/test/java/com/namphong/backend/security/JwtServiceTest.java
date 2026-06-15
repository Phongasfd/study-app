package com.namphong.backend.security;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    // test case: generate token and parse it back to get user id 
    @Test
    void generateAndParseToken() {
        // yêu cầu secret key đủ dài
        byte[] key = new byte[64]; // tạo mảngg byte có độ dài 64 bytes (512 bits) 
        new SecureRandom().nextBytes(key); // điền ngẫu nhiên dữ liệu vào mảng byte để tạo secret key an toàn
        String secret = Base64.getEncoder().encodeToString(key); // mã hóa secret key thành chuỗi Base64 để dễ dàng sử dụng trong cấu hình và lưu trữ

        JwtService jwtService = new JwtService(secret);
        UUID userId = UUID.randomUUID();

        // tạo jwt 
        String token = jwtService.generateToken(userId);
        // assert token không null và có thể parse lại được user id
        assertNotNull(token);

        // parse token để lấy user id
        UUID parsed = jwtService.getUserId(token);

        // assert user id ban đầu và user id sau khi parse phải giống nhau 
        assertEquals(userId, parsed);
    }
}
