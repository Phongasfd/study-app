package com.namphong.backend.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Subscribes to Redis Pub/Sub without making startup depend on Redis.
 *
 * RedisMessageListenerContainer subscribes when it starts. Started by the application context, a
 * Redis that was down, restarting or misnamed failed the whole context, and with
 * {@code restart: always} the backend crash-looped until Redis came back. The container is now
 * created with auto-startup off and started from here, on a background thread that waits for
 * Redis. Once subscribed, the container's own recovery handles later disconnects.
 *
 * Until then this node serves REST as usual and delivers events to its own sockets
 * (RedisGroupEventBus falls back to local delivery); events from other nodes arrive once subscribed.
 */
@Slf4j
public class RedisSubscriptionStarter implements SmartLifecycle {

    private static final long FIRST_DELAY_MS = 500;
    private static final long MAX_DELAY_MS = 30_000;

    private final RedisMessageListenerContainer container;
    private final RedisConnectionFactory connectionFactory;
    private volatile boolean running;
    private Thread thread;

    public RedisSubscriptionStarter(RedisMessageListenerContainer container, RedisConnectionFactory connectionFactory) {
        this.container = container;
        this.connectionFactory = connectionFactory;
    }

    @Override
    public void start() {
        running = true;
        thread = new Thread(this::subscribeWhenRedisIsUp, "redis-subscribe");
        thread.setDaemon(true);
        thread.start();
    }

    private void subscribeWhenRedisIsUp() {
        long delayMs = FIRST_DELAY_MS;
        while (running && !container.isRunning()) {
            try {
                // only start the container once Redis answers, so a failed start is the rare race
                try (RedisConnection connection = connectionFactory.getConnection()) {
                    connection.ping();
                }
                container.start();
                log.info("Subscribed to Redis Pub/Sub {}", RedisGroupEventBus.CHANNEL_PATTERN);
                return;
            } catch (RuntimeException e) {
                log.warn("Redis unavailable, retrying the Pub/Sub subscription in {} ms: {}", delayMs, e.getMessage());
            }
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            delayMs = Math.min(delayMs * 2, MAX_DELAY_MS);
        }
    }

    @Override
    public void stop() {
        running = false;
        if (thread != null) {
            thread.interrupt();
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
