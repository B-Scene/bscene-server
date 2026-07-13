package com.umc.bscene.domain.chat.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.bscene.domain.chat.dto.request.ChatMessageSendRequest;
import com.umc.bscene.domain.chat.dto.request.ChatWebSocketFrame;
import com.umc.bscene.domain.chat.dto.response.ChatMessageSendResult;
import com.umc.bscene.domain.chat.dto.response.ChatWebSocketPushFrame;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler implements SubProtocolCapable {
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
        ChatMessageSendResult result = chatMessageService.send(userId, request);
        String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);

        ChatWebSocketPushFrame senderFrame = new ChatWebSocketPushFrame(
                "dm.message",
                result.message().chatMessageId(),
                result.message(),
                frame.clientMsgId(),
                timestamp
        );
        ChatWebSocketPushFrame recipientFrame = new ChatWebSocketPushFrame(
                "dm.message",
                result.message().chatMessageId(),
                result.message(),
                null,
                timestamp
        );

        sessionRegistry.sendToUser(userId, new TextMessage(
                objectMapper.writeValueAsString(senderFrame)));
        sessionRegistry.sendToUser(result.recipientId(), new TextMessage(
                objectMapper.writeValueAsString(recipientFrame)));
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
