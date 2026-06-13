package com.namphong.backend.config;

import com.namphong.backend.websocket.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket // enable native websocket 
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer { // implements interface

    private final ChatWebSocketHandler handler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) // register for websocket endpoints like REST 
    {
        registry.addHandler(handler, "/ws")
                .setAllowedOrigins("*");
    }

}
