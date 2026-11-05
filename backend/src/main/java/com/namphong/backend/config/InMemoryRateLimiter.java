package com.namphong.backend.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/** Bucket4j token bucket per client, kept in this JVM. Correct only with a single backend instance. */
public class InMemoryRateLimiter implements RateLimiter {

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

    public InMemoryRateLimiter(RateLimiterProperties properties) {
        this.properties = properties;
    }

    // create bucket for each IP (key)
    private ClientBucket resolveBucket(String key) {
        return buckets.computeIfAbsent(key, k -> new ClientBucket(Bucket4j.builder()
                .addLimit(Bandwidth.simple(properties.getRequests(), Duration.ofSeconds(properties.getDurationSeconds())))
                .build()));
    }

    @Override
    public boolean tryAcquire(String clientKey) {
        long now = System.currentTimeMillis();
        if (buckets.size() > SWEEP_THRESHOLD) {
            evictIdleBuckets(now);
        }
        ClientBucket client = resolveBucket(clientKey);
        client.lastSeenMillis = now;
        return client.bucket.tryConsume(1); // còn token thì tiêu thụ 1 token
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
}
