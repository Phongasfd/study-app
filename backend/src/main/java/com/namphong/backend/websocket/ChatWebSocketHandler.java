package com.namphong.backend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.namphong.backend.dto.ChatMessageRequest;
import com.namphong.backend.dto.ChatMessageResponse;
import com.namphong.backend.dto.GroupRankingResponse;
import com.namphong.backend.entity.UserEntity;
import com.namphong.backend.repository.UserRepository;
import com.namphong.backend.service.ChatMessageService;
import com.namphong.backend.service.GroupRankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler { // class handling websocket text

    private final ChatMessageService chatMessageService; // save message to db
    private final GroupRankingService groupRankingService; // to send realtime ranking board
    private final UserRepository userRepository; // for find user
    private final ObjectMapper objectMapper; // to convert json to java object

    // groupId -> active sessions
    private static final ConcurrentHashMap<UUID, Set<WebSocketSession>> groupSessions = new ConcurrentHashMap<>();
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
    // "ws://localhost:8080/ws?groupId=123&userId=456"
    // )


    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Map<String, String> params = getQueryParams(session);
        // Get:
        // groupId=123
        // userId=456

        String groupIdStr = params.get("groupId"); // get groupId 

        if (groupIdStr != null) {
            try {
                // Convert String -> UUID 
                UUID groupId = UUID.fromString(groupIdStr);

                // if groupId not exists -> Tạo mới: GroupA → [] rồi: GroupA → [Session]
                groupSessions.computeIfAbsent(groupId, k -> ConcurrentHashMap.newKeySet()).add(session);
                
                // store groupId in session
                session.getAttributes().put("groupId", groupId);
                
                // Store userId in attributes as well if available
                String userIdStr = params.get("userId");
                if (userIdStr != null) {
                    session.getAttributes().put("userId", UUID.fromString(userIdStr));
                }
                
                System.out.println("WebSocket connection established for group: " + groupId + ", Session: " + session.getId());
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid UUID for groupId: " + groupIdStr);
                session.close(CloseStatus.BAD_DATA);
            }
        } else {
            System.err.println("No groupId provided in query params");
            session.close(CloseStatus.BAD_DATA);
        }
    }


    // when receiving messages 
    // Called when:
    // Frontend:

    // socket.send(
    // '{"content":"Hello"}'
    // )
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // get group id
        UUID groupId = (UUID) session.getAttributes().get("groupId"); 


        if (groupId == null) {
            return;
        }

        // Get sender userId from principal or session attributes fallback
        UUID userId = null;
        if (session.getPrincipal() != null) { // if user is authenticated in spring security
            try {
                userId = UUID.fromString(session.getPrincipal().getName());
            } catch (Exception ignored) {}
        }
        if (userId == null) {
            userId = (UUID) session.getAttributes().get("userId"); // fallback get userId from attributes
        }

        // user is not authenticated
        if (userId == null) {
            System.err.println("Unauthenticated WebSocket message from session: " + session.getId());
            return;
        }
        
        // find the sender by userId from db
        UserEntity sender = userRepository.findById(userId).orElse(null);
        if (sender == null) {
            System.err.println("Sender UserEntity not found for id: " + userId);
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

            // Broadcast message to group
            broadcastToGroup(groupId, Map.of(
                "type", "chat",
                "data", response
            ));
            // {
            //     "type": "chat", // distinguish the type of message
            //     "data": {
            //         ...
            //     } 
            // }
        } catch (Exception e) {
            System.err.println("Failed to handle WebSocket message: " + e.getMessage());
        }
    }

    // Run when a session is disconnected 
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // get group id from session
        UUID groupId = (UUID) session.getAttributes().get("groupId");
        
        // if group id is not null
        if (groupId != null) {
            // get sessions from group id 
            Set<WebSocketSession> sessions = groupSessions.get(groupId);
            // if sessions is not null
            if (sessions != null) {
                // remove session from group
                sessions.remove(session);
                
                // if sessions is empty -> remove group id 
                if (sessions.isEmpty()) {
                    groupSessions.remove(groupId);
                }
            }
        }
        // print message when session is disconnected
        System.out.println("WebSocket connection closed for session: " + session.getId());
    }

    // Broadcast ranking to group            
    public void broadcastRanking(UUID groupId) {
        try {
            // find group ranking by group id
            List<GroupRankingResponse> rankings = groupRankingService.findGroupRankingById(groupId);
            
            // broadcast ranking to group
            broadcastToGroup(groupId, Map.of(
                "type", "ranking",
                "data", rankings
            ));
        } catch (Exception e) {
            System.err.println("Failed to broadcast ranking for group " + groupId + ": " + e.getMessage());
        }
    }

    // broadcast message to group                                   
    private void broadcastToGroup(UUID groupId, Object messageObj) {
        // get sessions from group id 
        Set<WebSocketSession> sessions = groupSessions.get(groupId);
        // if sessions is null or empty -> return
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        try {
            // convert message to json
            String jsonPayload = objectMapper.writeValueAsString(messageObj);
            // create text message
            TextMessage textMessage = new TextMessage(jsonPayload);
            // send message to all sessions
            for (WebSocketSession session : sessions) {
                // if session is open 
                if (session.isOpen()) {
                    try {
                        // send message to session  
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        // print error message when failed to send message
                        System.err.println("Failed to send message to session " + session.getId() + ": " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to serialize WebSocket broadcast message: " + e.getMessage());
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
