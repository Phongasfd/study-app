package com.namphong.backend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.namphong.backend.dto.ChatMessageResponse;
import com.namphong.backend.entity.UserEntity;
import com.namphong.backend.repository.UserRepository;
import com.namphong.backend.service.ChatMessageService;
import com.namphong.backend.service.GroupMemberService;
import com.namphong.backend.service.GroupRankingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChatWebSocketHandlerTest {

    private GroupMemberService groupMemberService;
    private ChatMessageService chatMessageService;
    private UserRepository userRepository;
    private ChatWebSocketHandler handler;

    private final UUID groupId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        groupMemberService = mock(GroupMemberService.class);
        chatMessageService = mock(ChatMessageService.class);
        userRepository = mock(UserRepository.class);
        handler = new ChatWebSocketHandler(chatMessageService, mock(GroupRankingService.class),
                groupMemberService, userRepository, new ObjectMapper());
    }

    private WebSocketSession session(Principal principal, String query) {
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();
        when(session.getAttributes()).thenReturn(attributes);
        when(session.getPrincipal()).thenReturn(principal);
        when(session.getUri()).thenReturn(URI.create("ws://localhost/ws?" + query));
        when(session.getId()).thenReturn(UUID.randomUUID().toString());
        return session;
    }

    // JwtFilter puts the userId UUID as principal, so getName() is the UUID string
    private Principal principalOf(UUID id) {
        return new UsernamePasswordAuthenticationToken(id, null, List.of());
    }

    @Test
    void rejectsHandshakeWithoutAuthentication() throws Exception {
        // the old handler trusted ?userId=..., letting anyone post as anyone
        WebSocketSession session = session(null, "groupId=" + groupId + "&userId=" + userId);

        handler.afterConnectionEstablished(session);

        verify(session).close(CloseStatus.POLICY_VIOLATION);
        verify(groupMemberService, never()).isMember(any(), any());
    }

    @Test
    void rejectsAuthenticatedNonMember() throws Exception {
        when(groupMemberService.isMember(groupId, userId)).thenReturn(false);
        WebSocketSession session = session(principalOf(userId), "groupId=" + groupId);

        handler.afterConnectionEstablished(session);

        verify(session).close(CloseStatus.POLICY_VIOLATION);
    }

    @Test
    void memberMessageIsSavedAsTheAuthenticatedUser() throws Exception {
        UUID someoneElse = UUID.randomUUID();
        UserEntity me = UserEntity.builder().id(userId).username("me").build();
        when(groupMemberService.isMember(groupId, userId)).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(me));
        when(chatMessageService.createMessage(any(), any())).thenReturn(ChatMessageResponse.builder().content("hi").build());
        // a spoofed userId in the URL must be ignored
        WebSocketSession session = session(principalOf(userId), "groupId=" + groupId + "&userId=" + someoneElse);

        handler.afterConnectionEstablished(session);
        handler.handleTextMessage(session, new TextMessage("{\"content\":\"hi\"}"));

        verify(session, never()).close(any());
        verify(chatMessageService).createMessage(any(), eq(me));
        verify(userRepository, never()).findById(someoneElse);
    }

    @Test
    void messagesStopAfterUserLeavesGroup() throws Exception {
        when(groupMemberService.isMember(groupId, userId)).thenReturn(true, false);
        WebSocketSession session = session(principalOf(userId), "groupId=" + groupId);
        handler.afterConnectionEstablished(session);

        handler.handleTextMessage(session, new TextMessage("{\"content\":\"hi\"}"));

        verify(session).close(CloseStatus.POLICY_VIOLATION);
        verify(chatMessageService, never()).createMessage(any(), any());
    }

    private static <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
