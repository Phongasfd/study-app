package com.namphong.backend.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter { // đảm bảo filter này chỉ được thực thi một lần cho mỗi request

    // Map để lưu trữ các bucket cho mỗi client (dựa trên IP) 
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>(); // thread-safe 

    private final RateLimiterProperties properties;

    @Autowired
    public RateLimitingFilter(RateLimiterProperties properties) {
        this.properties = properties;
    } // inject properties từ AppConfig để sử dụng trong filter 

    // create bucket for each IP (key)
    private Bucket resolveBucket(String key) {
        return buckets.computeIfAbsent(key, k -> Bucket4j.builder()
                .addLimit(Bandwidth.simple(properties.getRequests(), Duration.ofSeconds(properties.getDurationSeconds())))
                .build());
    }

    // Kiểm tra bucket trước khi xử lý request. Nếu bucket còn token, cho phép request tiếp tục. Nếu không, trả về lỗi 429.
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String clientId = getClientIP(request);
        Bucket bucket = resolveBucket(clientId);

        if (bucket.tryConsume(1)) { // Nếu còn token, tiêu thụ 1 token và tiếp tục xử lý request
            filterChain.doFilter(request, response); // tiếp tục chuỗi filter và xử lý request
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value()); 
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Too many requests - rate limit exceeded\"}");
        }
    }

    // for getting client IP 
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) { // Nếu không có header "X-Forwarded-For", sử dụng IP trực tiếp từ request
            return request.getRemoteAddr(); // lấy IP của client từ request
        }
        return xfHeader.split(",")[0].trim(); // Nếu có header "X-Forwarded-For", lấy IP đầu tiên (client gốc) và loại bỏ khoảng trắng
    }
}
