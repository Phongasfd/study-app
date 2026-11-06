package com.namphong.backend.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.SessionLimitExceededException;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The WebSocket sessions connected to THIS node, per group.
 *
 * A WebSocket is a TCP connection to one JVM, so this map can only ever hold local sessions.
 * With several backend instances, a message must first reach every node (GroupEventBus)
 * and each node then writes it to its own sessions here.
 */
@Slf4j
@Component
public class GroupSessionRegistry {

    // A client that stops reading (bad network, frozen tab) must not block the thread that
    // delivers to everyone else: after 5 s of blocked sending or 512 KB of queued messages
    // the decorator gives up on that session, deliverLocally closes it, and the frontend reconnects.
    static final int SEND_TIME_LIMIT_MS = 5_000;
    static final int BUFFER_SIZE_LIMIT_BYTES = 512 * 1024;

    // groupId -> (sessionId -> session) open on this node. Sessions are stored wrapped in
    // ConcurrentWebSocketSessionDecorator: raw sendMessage is not thread-safe, and chat and
    // ranking broadcasts can reach the same session from different threads.
    private final ConcurrentHashMap<UUID, Map<String, WebSocketSession>> groupSessions = new ConcurrentHashMap<>();

    // compute/computeIfPresent run atomically per key: a session added while the last one
    // leaves can't end up in a map that was already removed from groupSessions
    public void add(UUID groupId, WebSocketSession session) {
        WebSocketSession concurrent = new ConcurrentWebSocketSessionDecorator(
                session, SEND_TIME_LIMIT_MS, BUFFER_SIZE_LIMIT_BYTES);
        groupSessions.compute(groupId, (id, sessions) -> {
            Map<String, WebSocketSession> map = sessions != null ? sessions : new ConcurrentHashMap<>();
            map.put(session.getId(), concurrent);
            return map;
        });
    }

    public void remove(UUID groupId, WebSocketSession session) {
        groupSessions.computeIfPresent(groupId, (id, sessions) -> {
            sessions.remove(session.getId());
            return sessions.isEmpty() ? null : sessions;
        });
    }

    public int localSessionCount(UUID groupId) {
        Map<String, WebSocketSession> sessions = groupSessions.get(groupId);
        return sessions == null ? 0 : sessions.size();
    }

    /** Writes an already-serialized message to every open session of the group on this node. */
    public int deliverLocally(UUID groupId, String json) {
        Map<String, WebSocketSession> sessions = groupSessions.get(groupId);
        if (sessions == null || sessions.isEmpty()) {
            return 0;
        }
        TextMessage textMessage = new TextMessage(json);
        int delivered = 0;
        for (WebSocketSession session : sessions.values()) {
            if (!session.isOpen()) {
                continue;
            }
            try {
                session.sendMessage(textMessage);
                delivered++;
            } catch (SessionLimitExceededException e) {
                // The decorator only flags the session and throws; it never closes it. Left open,
                // the socket stayed registered but silently received nothing more, and the client
                // never reconnected. Drop it now and close it off this thread: the close can wait
                // behind the write that is stuck.
                log.warn("Closing slow session {}: {}", session.getId(), e.getMessage());
                remove(groupId, session);
                CompletableFuture.runAsync(() -> closeQuietly(session, e.getStatus()));
            } catch (IOException | RuntimeException e) {
                log.warn("Failed to send message to session {}: {}", session.getId(), e.getMessage());
            }
        }
        return delivered;
    }

    private static void closeQuietly(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (IOException | RuntimeException e) {
            log.debug("Closing session {} failed: {}", session.getId(), e.getMessage());
        }
    }
}
