package com.namphong.backend.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;

/**
 * Sends a real-time event to everyone connected to a group, whichever node they are on.
 *
 * <ul>
 *   <li>{@link LocalGroupEventBus}: single instance, writes straight to local sessions (default).</li>
 *   <li>{@link RedisGroupEventBus}: several instances, fans out through Redis Pub/Sub
 *       ({@code app.redis.enabled=true}).</li>
 * </ul>
 */
public interface GroupEventBus {

    /** Publishes {@code {"type": type, "data": data}} to every member socket of the group. */
    void publish(UUID groupId, String type, Object data);

    /** The envelope the frontend expects; type tells chat and ranking apart on the shared socket. */
    record Envelope(String type, Object data) {
    }

    static String toJson(ObjectMapper objectMapper, String type, Object data) {
        try {
            return objectMapper.writeValueAsString(new Envelope(type, data));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize " + type + " event", e);
        }
    }
}
