package com.namphong.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.namphong.backend.websocket.GroupEventBus;
import com.namphong.backend.websocket.GroupSessionRegistry;
import com.namphong.backend.websocket.LocalGroupEventBus;
import com.namphong.backend.websocket.RedisGroupEventBus;
import com.namphong.backend.websocket.RedisSubscriptionStarter;
import io.lettuce.core.ClientOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.data.redis.autoconfigure.LettuceClientOptionsBuilderCustomizer;
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
 *
 * The two conditions are exact complements: before, a value such as "" or "1" matched neither,
 * no RateLimiter bean existed and the backend did not start.
 */
@Configuration
public class ScalingConfig {

    static final String REDIS_ENABLED = "'${app.redis.enabled:false}'.trim().equalsIgnoreCase('true')";

    @Configuration
    @ConditionalOnExpression("!" + REDIS_ENABLED)
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
    @ConditionalOnExpression(REDIS_ENABLED)
    static class MultiInstance {

        @Bean
        RedisGroupEventBus redisGroupEventBus(StringRedisTemplate redis, GroupSessionRegistry registry,
                                              ObjectMapper objectMapper) {
            return new RedisGroupEventBus(redis, registry, objectMapper);
        }

        // Every node subscribes to all groups' channels and keeps the messages for its own sockets.
        // The dispatcher thread is deliberately not a bean: an Executor bean would replace
        // Spring Boot's default applicationTaskExecutor.
        // Not started by the context: with Redis down that start failed the whole application.
        @Bean
        RedisMessageListenerContainer groupEventListenerContainer(RedisConnectionFactory connectionFactory,
                                                                  RedisGroupEventBus bus) {
            RedisMessageListenerContainer container = RedisGroupEventBus.listenerContainer(connectionFactory, bus,
                    RedisGroupEventBus.singleThreadDispatcher());
            container.setAutoStartup(false);
            return container;
        }

        @Bean
        RedisSubscriptionStarter redisSubscriptionStarter(RedisMessageListenerContainer groupEventListenerContainer,
                                                          RedisConnectionFactory connectionFactory) {
            return new RedisSubscriptionStarter(groupEventListenerContainer, connectionFactory);
        }

        // While Redis is unreachable, fail commands at once instead of queueing them until the
        // command timeout (60 s by default): the rate limiter runs on every request, so queued
        // commands stalled the whole API. With this the limiter fails open and publish falls back
        // to local delivery immediately. application-prod.yml also sets short timeouts.
        @Bean
        LettuceClientOptionsBuilderCustomizer rejectRedisCommandsWhileDisconnected() {
            return options -> options.disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS);
        }

        @Bean
        RateLimiter redisRateLimiter(StringRedisTemplate redis, RateLimiterProperties properties) {
            return new RedisRateLimiter(redis, properties);
        }
    }
}
