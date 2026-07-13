package com.umc.bscene.domain.chat.websocket;

import com.umc.bscene.domain.chat.service.ChatWebSocketTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Arrays;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ChatWebSocketHandshakeInterceptor implements HandshakeInterceptor {
    public static final String USER_ID_ATTRIBUTE = "chatWebSocketUserId";
    private static final String SUBPROTOCOL = "dm.v1";
    private static final String SEC_WEBSOCKET_PROTOCOL = "Sec-WebSocket-Protocol";

    private final ChatWebSocketTicketService ticketService;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) return false;
        if (!supportsDmSubprotocol(request.getHeaders())) return false;

        String ticket = servletRequest.getServletRequest().getParameter("ticket");
        Long userId = ticketService.consume(ticket);
        if (userId == null) return false;

        attributes.put(USER_ID_ATTRIBUTE, userId);
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
    }

    private boolean supportsDmSubprotocol(HttpHeaders headers) {
        return headers.getOrEmpty(SEC_WEBSOCKET_PROTOCOL).stream()
                .flatMap(value -> Arrays.stream(value.split(",")))
                .map(String::trim)
                .anyMatch(SUBPROTOCOL::equals);
    }
}
