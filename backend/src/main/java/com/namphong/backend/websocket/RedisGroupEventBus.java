package com.namphong.backend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Multi-instance delivery through Redis Pub/Sub.
 *
 * <pre>
 *   node A: publish(group G)  --PUBLISH study:group:G-->  Redis
 *   Redis   --pattern study:group:*-->  node A, node B, ...  (every node, including A)
 *   each node: write the message to ITS sessions of group G (GroupSessionRegistry)
 * </pre>
 *
 * publish() never writes to local sessions itself: the node receives its own message back
 * from Redis like every other node, so each socket gets the message exactly once.
 *
 * The payload is serialized once on the publishing node and forwarded as-is, so the
 * ranking is computed once per change, not once per node.
 *
 * Trade-offs: Pub/Sub is fire-and-forget (a node that is reconnecting to Redis misses
 * messages; the client reloads history and the next ranking push corrects the board),
 * and every node receives every group's events, even groups with no local sockets.
 * Fine at this scale; per-group SUBSCRIBE or Redis Streams would be the next steps.
 */
@Slf4j
public class RedisGroupEventBus implements GroupEventBus, MessageListener {

    static final String CHANNEL_PREFIX = "study:group:";
    public static final String CHANNEL_PATTERN = CHANNEL_PREFIX + "*";

    private final StringRedisTemplate redis;
    private final GroupSessionRegistry registry;
    private final ObjectMapper objectMapper;

    public RedisGroupEventBus(StringRedisTemplate redis, GroupSessionRegistry registry, ObjectMapper objectMapper) {
        this.redis = redis;
        this.registry = registry;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(UUID groupId, String type, Object data) {
        String json = GroupEventBus.toJson(objectMapper, type, data);
        try {
            redis.convertAndSend(CHANNEL_PREFIX + groupId, json);
        } catch (RuntimeException e) {
            // Redis down: degrade to single-node delivery rather than dropping the event for everyone
            log.error("Redis publish failed for group {}, delivering to local sessions only: {}", groupId, e.getMessage());
            registry.deliverLocally(groupId, json);
        }
    }

    /**
     * The subscription every node runs. Redis delivers a channel's messages in publish order,
     * but the container's default executor starts a new thread per message, so two chat
     * messages sent back to back could reach the sockets swapped. A single dispatcher thread
     * keeps the order; it can't be stalled by a slow client because GroupSessionRegistry
     * bounds every socket's send time and buffer.
     */
    public static RedisMessageListenerContainer listenerContainer(RedisConnectionFactory connectionFactory,
                                                                  RedisGroupEventBus bus,
                                                                  Executor dispatcher) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setTaskExecutor(dispatcher);
        container.addMessageListener(bus, new PatternTopic(CHANNEL_PATTERN));
        return container;
    }

    /** One daemon thread: messages are handled strictly one after another, in arrival order. */
    public static ExecutorService singleThreadDispatcher() {
        return Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "group-events");
            thread.setDaemon(true);
            return thread;
        });
    }

    /** Called by RedisMessageListenerContainer for every message on study:group:*. */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        UUID groupId;
        try {
            groupId = UUID.fromString(channel.substring(CHANNEL_PREFIX.length()));
        } catch (RuntimeException e) {
            log.warn("Ignoring message on unexpected channel {}", channel);
            return;
        }
        registry.deliverLocally(groupId, new String(message.getBody(), StandardCharsets.UTF_8));
    }
}
