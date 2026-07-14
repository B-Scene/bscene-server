package com.umc.bscene.domain.chat.websocket;

import com.umc.bscene.domain.chat.service.LiveChatWebSocketTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
public class LiveChatWebSocketHandshakeInterceptor implements HandshakeInterceptor {
    public static final String USER_ID_ATTRIBUTE = "liveChatUserId";
    public static final String LIVE_ID_ATTRIBUTE = "liveChatLiveId";
    private static final String SUBPROTOCOL = "live-chat.v1";

    private final LiveChatWebSocketTicketService ticketService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)
                || !supportsSubprotocol(request.getHeaders())) {
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return false;
        }

        LiveChatWebSocketTicketService.TicketPrincipal principal =
                ticketService.consume(servletRequest.getServletRequest().getParameter("ticket"));
        Long pathLiveId = extractLiveId(request.getURI().getPath());
        if (principal == null || pathLiveId == null || !pathLiveId.equals(principal.liveId())) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        attributes.put(USER_ID_ATTRIBUTE, principal.userId());
        attributes.put(LIVE_ID_ATTRIBUTE, principal.liveId());
        return true;
    }

    private boolean supportsSubprotocol(HttpHeaders headers) {
        return headers.getOrEmpty("Sec-WebSocket-Protocol").stream()
                .flatMap(value -> Arrays.stream(value.split(",")))
                .map(String::trim)
                .anyMatch(SUBPROTOCOL::equals);
    }

    private Long extractLiveId(String path) {
        String marker = "/ws/lives/";
        int start = path.indexOf(marker);
        int end = path.indexOf("/chat", start + marker.length());
        if (start < 0 || end < 0) return null;
        try {
            return Long.valueOf(path.substring(start + marker.length(), end));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
