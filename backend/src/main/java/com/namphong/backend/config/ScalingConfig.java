package com.namphong.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.namphong.backend.websocket.GroupEventBus;
import com.namphong.backend.websocket.GroupSessionRegistry;
import com.namphong.backend.websocket.LocalGroupEventBus;
import com.namphong.backend.websocket.RedisGroupEventBus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * The two pieces of per-instance state, and how they are shared when there is more than one instance:
 *
 * <table>
 *   <tr><th></th><th>app.redis.enabled=false (default)</th><th>app.redis.enabled=true</th></tr>
 *   <tr><td>WebSocket fan-out</td><td>LocalGroupEventBus</td><td>RedisGroupEventBus (Pub/Sub)</td></tr>
 *   <tr><td>Rate limit</td><td>InMemoryRateLimiter</td><td>RedisRateLimiter</td></tr>
 * </table>
 *
 * Everything else is already shared: JWTs are stateless, refresh tokens live in PostgreSQL,
 * chat history in MongoDB. The Google OAuth2 handshake keeps its state in the HTTP session,
 * so nginx pins only those two endpoints to one node (ip_hash), see nginx.conf.
 */
@Configuration
public class ScalingConfig {

    @Configuration
    @ConditionalOnProperty(name = "app.redis.enabled", havingValue = "false", matchIfMissing = true)
    static class SingleInstance {

        @Bean
        GroupEventBus localGroupEventBus(GroupSessionRegistry registry, ObjectMapper objectMapper) {
            return new LocalGroupEventBus(registry, objectMapper);
        }

        @Bean
        RateLimiter inMemoryRateLimiter(RateLimiterProperties properties) {
            return new InMemoryRateLimiter(properties);
        }
    }

    @Configuration
    @ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
    static class MultiInstance {

        @Bean
        RedisGroupEventBus redisGroupEventBus(StringRedisTemplate redis, GroupSessionRegistry registry,
                                              ObjectMapper objectMapper) {
            return new RedisGroupEventBus(redis, registry, objectMapper);
        }

        // Every node subscribes to all groups' channels and keeps the messages for its own sockets.
        // The dispatcher thread is deliberately not a bean: an Executor bean would replace
        // Spring Boot's default applicationTaskExecutor.
        @Bean
        RedisMessageListenerContainer groupEventListenerContainer(RedisConnectionFactory connectionFactory,
                                                                  RedisGroupEventBus bus) {
            return RedisGroupEventBus.listenerContainer(connectionFactory, bus,
                    RedisGroupEventBus.singleThreadDispatcher());
        }

        @Bean
        RateLimiter redisRateLimiter(StringRedisTemplate redis, RateLimiterProperties properties) {
            return new RedisRateLimiter(redis, properties);
        }
    }
}
