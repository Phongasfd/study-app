package com.namphong.backend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.namphong.backend.dto.ChatMessageRequest;
import com.namphong.backend.dto.ChatMessageResponse;
import com.namphong.backend.entity.UserEntity;
import com.namphong.backend.repository.UserRepository;
import com.namphong.backend.service.ChatMessageService;
import com.namphong.backend.service.GroupMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.security.Principal;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler { // class handling websocket text

    private final ChatMessageService chatMessageService; // save message to db
    private final GroupMemberService groupMemberService; // only members may join a group's socket
    private final UserRepository userRepository; // for find user
    private final ObjectMapper objectMapper; // to convert json to java object
    private final GroupSessionRegistry sessionRegistry; // groupId -> sessions open on THIS node
    private final GroupEventBus groupEventBus; // reaches the group's sockets on every node (local or Redis)

    // groupId -> active sessions now lives in GroupSessionRegistry (one per node).
    // With 2+ backend instances, members of one group are spread over different JVMs,
    // so a broadcast goes through GroupEventBus (Redis Pub/Sub) instead of this map alone.
    // // handle online users
    // It stores in RAM:
    // {
    // GroupA :
    // [Session1, Session2, Session3],

    // GroupB :
    // [Session4, Session5]
    // }
    // Why ConcurrentHashMap?

    // When multiple users chat

    // User A send
    // User B send
    // User C disconnect
    // at the same time

    // HashMap
    // Not thread-safe.

    // ConcurrentHashMap
    // Thread-safe.


    // when client connected
    // Called when:
    // Frontend:
    // new WebSocket(
    // "ws://localhost:8080/ws?groupId=123"
    // )
    // The user is NOT taken from the URL: the handshake is an HTTP request, so JwtFilter
    // reads the access_token cookie and Spring Security exposes it as session.getPrincipal().


    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        UUID userId = getAuthenticatedUserId(session);
        if (userId == null) {
            log.warn("Rejecting unauthenticated WebSocket session {}", session.getId());
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        String groupIdStr = getQueryParams(session).get("groupId"); // get groupId
        if (groupIdStr == null) {
            log.warn("No groupId provided in query params");
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        UUID groupId;
        try {
            // Convert String -> UUID
            groupId = UUID.fromString(groupIdStr);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID for groupId: {}", groupIdStr);
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        // Only members may read the chat / ranking stream of a group
        if (!groupMemberService.isMember(groupId, userId)) {
            log.warn("User {} is not a member of group {}, closing WebSocket", userId, groupId);
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        // if groupId not exists -> Tạo mới: GroupA → [] rồi: GroupA → [Session]
        sessionRegistry.add(groupId, session);

        // store groupId + authenticated userId in session
        session.getAttributes().put("groupId", groupId);
        session.getAttributes().put("userId", userId);

        log.info("WebSocket connection established for group: {}, session: {}", groupId, session.getId());
    }


    // when receiving messages
    // Called when:
    // Frontend:

    // socket.send(
    // '{"content":"Hello"}'
    // )
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // groupId + userId were set in afterConnectionEstablished, after authentication
        UUID groupId = (UUID) session.getAttributes().get("groupId");
        UUID userId = (UUID) session.getAttributes().get("userId");

        if (groupId == null || userId == null) {
            return;
        }

        // the user may have been removed from the group after connecting
        if (!groupMemberService.isMember(groupId, userId)) {
            log.warn("User {} left group {}, closing WebSocket", userId, groupId);
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        // find the sender by userId from db
        UserEntity sender = userRepository.findById(userId).orElse(null);
        if (sender == null) {
            log.warn("Sender UserEntity not found for id: {}", userId);
            return;
        }

        // get content from message
        String payload = message.getPayload();
        try {
            // convert payload to map
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
            // get content from map
            String content = (String) data.get("content");

            // if content is null or empty -> return
            if (content == null || content.trim().isEmpty()) {
                return;
            }

            // create chat message request
            ChatMessageRequest request = new ChatMessageRequest();
            request.setGroupId(groupId.toString()); // set group id
            request.setContent(content); // set content

            // save message to db
            ChatMessageResponse response = chatMessageService.createMessage(request, sender);

            // Broadcast message to group, on every backend instance
            groupEventBus.publish(groupId, "chat", response);
            // {
            //     "type": "chat", // distinguish the type of message
            //     "data": {
            //         ...
            //     }
            // }
        } catch (Exception e) {
            log.error("Failed to handle WebSocket message: {}", e.getMessage());
        }
    }

    // Run when a session is disconnected
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // get group id from session
        UUID groupId = (UUID) session.getAttributes().get("groupId");

        // if group id is not null -> remove session from group (and the group once it is empty)
        if (groupId != null) {
            sessionRegistry.remove(groupId, session);
        }
        log.info("WebSocket connection closed for session: {}", session.getId());
    }

    // userId comes only from the authenticated principal (JwtFilter sets it to the userId UUID)
    private UUID getAuthenticatedUserId(WebSocketSession session) {
        Principal principal = session.getPrincipal();
        if (principal == null) {
            return null;
        }
        try {
            return UUID.fromString(principal.getName());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // get query params from session
    private Map<String, String> getQueryParams(WebSocketSession session) {
        // get uri from session
        URI uri = session.getUri();
        // if uri is null or query is null -> return empty map
        if (uri == null || uri.getQuery() == null) {
            return Collections.emptyMap();
        }
        // create query params map
        Map<String, String> queryParams = new HashMap<>();
        // split query params by "&"
        String[] pairs = uri.getQuery().split("&");
        // for each pair
        for (String pair : pairs) {
            // get index of "="
            int idx = pair.indexOf("=");
            // if index is greater than 0
            if (idx > 0) {
                // put key-value to map
                queryParams.put(pair.substring(0, idx), pair.substring(idx + 1));
            } else {
                // put key with empty value to map
                queryParams.put(pair, "");
            }
        }
        // return query params map
        return queryParams;
    }
}
