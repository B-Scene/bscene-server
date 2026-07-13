package com.umc.bscene.domain.chat.config;

import com.umc.bscene.domain.chat.websocket.ChatWebSocketHandler;
import com.umc.bscene.domain.chat.websocket.ChatWebSocketHandshakeInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class ChatWebSocketConfig implements WebSocketConfigurer {
    private final ChatWebSocketHandler chatWebSocketHandler;
    private final ChatWebSocketHandshakeInterceptor handshakeInterceptor;
    private final String[] allowedOrigins;

    public ChatWebSocketConfig(
            ChatWebSocketHandler chatWebSocketHandler,
            ChatWebSocketHandshakeInterceptor handshakeInterceptor,
            @Value("${cors.allowed-origins}") String[] allowedOrigins
    ) {
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.handshakeInterceptor = handshakeInterceptor;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins(allowedOrigins);
    }
}
