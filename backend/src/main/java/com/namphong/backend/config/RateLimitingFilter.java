package com.namphong.backend.config;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@Component
public class RateLimitingFilter extends OncePerRequestFilter { // đảm bảo filter này chỉ được thực thi một lần cho mỗi request

    // in-memory (1 instance) hoặc Redis (nhiều instance), chọn trong ScalingConfig
    private final RateLimiter rateLimiter;

    public RateLimitingFilter(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    // Kiểm tra limit trước khi xử lý request. Nếu còn quota, cho phép request tiếp tục. Nếu không, trả về lỗi 429.
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (rateLimiter.tryAcquire(getClientIP(request))) {
            filterChain.doFilter(request, response); // tiếp tục chuỗi filter và xử lý request
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Too many requests - rate limit exceeded\"}");
        }
    }

    // for getting client IP
    private String getClientIP(HttpServletRequest request) {
        // Never read X-Forwarded-For here: the client controls its first entry, so any
        // value in it would give a fresh bucket. Behind nginx, the prod profile sets
        // server.forward-headers-strategy=framework and nginx overwrites the header with
        // $remote_addr, so getRemoteAddr() is already the real client IP.
        return request.getRemoteAddr();
    }
}
