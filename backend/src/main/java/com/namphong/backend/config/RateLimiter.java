package com.namphong.backend.config;

/**
 * Decides whether one more request from a client fits its limit.
 *
 * <ul>
 *   <li>{@link InMemoryRateLimiter}: per-JVM token buckets (single instance, default).</li>
 *   <li>{@link RedisRateLimiter}: one counter shared by all instances ({@code app.redis.enabled=true}).
 *       With in-memory buckets behind a load balancer, N instances would allow N x the limit.</li>
 * </ul>
 */
public interface RateLimiter {

    boolean tryAcquire(String clientKey);
}
