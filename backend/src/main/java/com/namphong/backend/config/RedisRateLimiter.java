package com.namphong.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;

/**
 * Fixed-window counter in Redis, shared by every backend instance.
 *
 * INCR and PEXPIRE run in one Lua script, so they are atomic: without that, a crash between
 * the two would leave a counter with no expiry and block the client forever.
 *
 * Trade-offs:
 * <ul>
 *   <li>Fixed window, not a token bucket: a client can send up to 2x the limit across a
 *       window boundary. Acceptable for abuse protection; a sliding window or GCRA script
 *       would remove it.</li>
 *   <li>Fails open: if Redis is unreachable the request is allowed (and logged). The limiter
 *       protects against abuse; it must not take the whole API down with Redis.</li>
 * </ul>
 */
@Slf4j
public class RedisRateLimiter implements RateLimiter {

    static final String KEY_PREFIX = "study:ratelimit:";

    private static final RedisScript<Long> INCREMENT_IN_WINDOW = new DefaultRedisScript<>(
            "local count = redis.call('INCR', KEYS[1]) " +
            "if count == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end " +
            "return count",
            Long.class);

    private final StringRedisTemplate redis;
    private final RateLimiterProperties properties;

    public RedisRateLimiter(StringRedisTemplate redis, RateLimiterProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    @Override
    public boolean tryAcquire(String clientKey) {
        String windowMillis = String.valueOf(Duration.ofSeconds(properties.getDurationSeconds()).toMillis());
        try {
            Long count = redis.execute(INCREMENT_IN_WINDOW, List.of(KEY_PREFIX + clientKey), windowMillis);
            return count == null || count <= properties.getRequests();
        } catch (RuntimeException e) {
            log.warn("Rate limiter unavailable (Redis), allowing request: {}", e.getMessage());
            return true;
        }
    }
}
