package com.umc.bscene.domain.chat.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.bscene.domain.chat.dto.request.ChatMessageSendRequest;
import com.umc.bscene.domain.chat.dto.request.ChatWebSocketFrame;
import com.umc.bscene.domain.chat.exception.ChatException;
import com.umc.bscene.domain.chat.response.code.ChatWebSocketErrorCode;
import com.umc.bscene.domain.chat.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.SubProtocolCapable;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler implements SubProtocolCapable {
    private final ChatWebSocketSessionRegistry sessionRegistry;
    private final ChatMessageService chatMessageService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<String> getSubProtocols() {
        return List.of("dm.v1");
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = (Long) session.getAttributes()
                .get(ChatWebSocketHandshakeInterceptor.USER_ID_ATTRIBUTE);
        sessionRegistry.register(userId, session);
        log.debug("Chat WebSocket connected: userId={}, sessionId={}", userId, session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        ChatWebSocketFrame frame = objectMapper.readValue(
                message.getPayload(), ChatWebSocketFrame.class);
        if (!"dm.send".equals(frame.type())
                || frame.data() == null
                || !isUuid(frame.clientMsgId())) {
            throw new ChatException(ChatWebSocketErrorCode.INVALID_FRAME);
        }

        ChatMessageSendRequest request = objectMapper.treeToValue(
                frame.data(), ChatMessageSendRequest.class);
        Long userId = (Long) session.getAttributes()
                .get(ChatWebSocketHandshakeInterceptor.USER_ID_ATTRIBUTE);
        chatMessageService.send(userId, request);
    }

    private boolean isUuid(String value) {
        if (value == null || value.isBlank()) return false;
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes()
                .get(ChatWebSocketHandshakeInterceptor.USER_ID_ATTRIBUTE);
        sessionRegistry.unregister(userId, session.getId());
        log.debug("Chat WebSocket closed: userId={}, sessionId={}, status={}",
                userId, session.getId(), status);
    }
}
