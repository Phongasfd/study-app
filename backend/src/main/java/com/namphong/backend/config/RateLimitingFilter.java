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

    // Khi số client vượt ngưỡng này thì dọn các bucket đã nhàn rỗi
    static final int SWEEP_THRESHOLD = 10_000;

    // bucket + lần cuối client gửi request (để biết bucket nào đã nhàn rỗi)
    private static final class ClientBucket {
        final Bucket bucket;
        volatile long lastSeenMillis;

        ClientBucket(Bucket bucket) {
            this.bucket = bucket;
        }
    }

    // Map để lưu trữ các bucket cho mỗi client (dựa trên IP)
    private final ConcurrentHashMap<String, ClientBucket> buckets = new ConcurrentHashMap<>(); // thread-safe

    private final RateLimiterProperties properties;

    @Autowired
    public RateLimitingFilter(RateLimiterProperties properties) {
        this.properties = properties;
    } // inject properties từ AppConfig để sử dụng trong filter

    // create bucket for each IP (key)
    private ClientBucket resolveBucket(String key) {
        return buckets.computeIfAbsent(key, k -> new ClientBucket(Bucket4j.builder()
                .addLimit(Bandwidth.simple(properties.getRequests(), Duration.ofSeconds(properties.getDurationSeconds())))
                .build()));
    }

    // Kiểm tra bucket trước khi xử lý request. Nếu bucket còn token, cho phép request tiếp tục. Nếu không, trả về lỗi 429.
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        long now = System.currentTimeMillis();
        if (buckets.size() > SWEEP_THRESHOLD) {
            evictIdleBuckets(now);
        }

        ClientBucket client = resolveBucket(getClientIP(request));
        client.lastSeenMillis = now;

        if (client.bucket.tryConsume(1)) { // Nếu còn token, tiêu thụ 1 token và tiếp tục xử lý request
            filterChain.doFilter(request, response); // tiếp tục chuỗi filter và xử lý request
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Too many requests - rate limit exceeded\"}");
        }
    }

    // A bucket idle for a full window has refilled completely, so dropping it
    // is the same as keeping it — the map stays bounded without weakening the limit.
    private void evictIdleBuckets(long now) {
        long windowMillis = Duration.ofSeconds(properties.getDurationSeconds()).toMillis();
        buckets.entrySet().removeIf(e -> now - e.getValue().lastSeenMillis > windowMillis);
    }

    int trackedClients() {
        return buckets.size();
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
