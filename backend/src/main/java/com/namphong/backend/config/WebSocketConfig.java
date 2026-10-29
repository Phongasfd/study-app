package com.namphong.backend.config;

import com.namphong.backend.websocket.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.List;

@Configuration
@EnableWebSocket // enable native websocket 
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer { // implements interface

    private final ChatWebSocketHandler handler;

    // Same allow-list as REST CORS: the handshake carries the auth cookie,
    // so "*" would let any site open a socket as the logged-in user
    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) // register for websocket endpoints like REST
    {
        registry.addHandler(handler, "/ws")
                .setAllowedOrigins(allowedOrigins.toArray(String[]::new));
    }

}
