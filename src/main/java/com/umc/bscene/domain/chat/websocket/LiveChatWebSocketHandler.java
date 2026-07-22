package com.umc.bscene.domain.chat.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.bscene.domain.chat.dto.request.ChatWebSocketFrame;
import com.umc.bscene.domain.chat.dto.request.LiveChatMessageSendRequest;
import com.umc.bscene.domain.chat.dto.response.ChatWebSocketErrorData;
import com.umc.bscene.domain.chat.dto.response.ChatWebSocketPushFrame;
import com.umc.bscene.domain.chat.dto.response.ChatWebSocketSystemEventData;
import com.umc.bscene.domain.chat.dto.response.LiveChatMessageData;
import com.umc.bscene.domain.chat.exception.ChatException;
import com.umc.bscene.domain.chat.enums.code.error.ChatErrorCode;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.repository.UserRepository;
import com.umc.bscene.domain.user.repository.UserBlockRepository;
import com.umc.bscene.domain.user.repository.FanProfileRepository;
import com.umc.bscene.domain.user.entity.FanProfile;
import com.umc.bscene.domain.stream.repository.ReportHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.SubProtocolCapable;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class LiveChatWebSocketHandler extends TextWebSocketHandler implements SubProtocolCapable {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_CONTENT_LENGTH = 500;

    private final LiveChatWebSocketSessionRegistry sessionRegistry;
    private final UserRepository userRepository;
    private final UserBlockRepository userBlockRepository;
    private final ReportHistoryRepository reportHistoryRepository;
    private final FanProfileRepository fanProfileRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<String> getSubProtocols() {
        return List.of("live-chat.v1");
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long liveId = liveId(session);
        sessionRegistry.register(liveId, userId(session), session);
        ChatWebSocketPushFrame frame = new ChatWebSocketPushFrame(
                "system.event", null, new ChatWebSocketSystemEventData("connected"),
                null, now());
        sessionRegistry.sendToSession(liveId, session.getId(),
                new TextMessage(objectMapper.writeValueAsString(frame)));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String clientMsgId = null;
        try {
            ChatWebSocketFrame frame = objectMapper.readValue(message.getPayload(), ChatWebSocketFrame.class);
            clientMsgId = frame.clientMsgId();
            if ("ping".equals(frame.type())) {
                handlePing(session, frame);
            } else if ("live-chat.send".equals(frame.type())) {
                handleSend(session, frame);
            } else {
                throw new ChatException(ChatErrorCode.LIVE_UNSUPPORTED_TYPE);
            }
        } catch (ChatException exception) {
            sendError(session, clientMsgId, (ChatErrorCode) exception.getBaseResponseCode());
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            sendError(session, clientMsgId, ChatErrorCode.LIVE_INVALID_FRAME);
        } catch (Exception exception) {
            log.error("Live chat handling failed: sessionId={}", session.getId(), exception);
            sendError(session, clientMsgId, ChatErrorCode.LIVE_INTERNAL_ERROR);
        }
    }

    private void handleSend(WebSocketSession session, ChatWebSocketFrame frame) throws Exception {
        if (!isUuid(frame.clientMsgId()) || frame.data() == null) {
            throw new ChatException(ChatErrorCode.LIVE_INVALID_FRAME);
        }
        LiveChatMessageSendRequest request = objectMapper.treeToValue(
                frame.data(), LiveChatMessageSendRequest.class);
        String content = request.content() == null ? "" : request.content().trim();
        if (content.isEmpty()) throw new ChatException(ChatErrorCode.LIVE_EMPTY_CONTENT);
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new ChatException(ChatErrorCode.LIVE_CONTENT_TOO_LONG);
        }

        Long userId = userId(session);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.LIVE_INVALID_FRAME));
        String senderName = fanProfileRepository.findByUser(user)
                .map(FanProfile::getNickname)
                .orElse(user.getName());
        String messageId = UUID.randomUUID().toString();
        LiveChatMessageData data = new LiveChatMessageData(
                messageId, liveId(session), userId, senderName, null, content, now());
        ChatWebSocketPushFrame push = new ChatWebSocketPushFrame(
                "live-chat.message", null, data, frame.clientMsgId(), now());
        Set<Long> excludedUserIds = new HashSet<>(
                userBlockRepository.findBlockedUserIdsRelatedTo(liveId(session), userId));
        excludedUserIds.addAll(reportHistoryRepository
                .findReporterIdsByLiveIdAndTargetUserId(liveId(session), userId));
        sessionRegistry.broadcastExcept(liveId(session), excludedUserIds,
                new TextMessage(objectMapper.writeValueAsString(push)));
    }

    private void handlePing(WebSocketSession session, ChatWebSocketFrame frame) throws Exception {
        if (frame.clientMsgId() != null || frame.data() == null || !frame.data().isObject()) {
            throw new ChatException(ChatErrorCode.LIVE_INVALID_FRAME);
        }
        ChatWebSocketPushFrame pong = new ChatWebSocketPushFrame(
                "pong", null, objectMapper.createObjectNode(), null, now());
        sessionRegistry.sendToSession(liveId(session), session.getId(),
                new TextMessage(objectMapper.writeValueAsString(pong)));
    }

    private void sendError(WebSocketSession session, String clientMsgId,
                           ChatErrorCode code) {
        ChatWebSocketPushFrame frame = new ChatWebSocketPushFrame(
                "system.error", null, new ChatWebSocketErrorData(code.getCode(), code.getMessage()),
                clientMsgId, now());
        try {
            sessionRegistry.sendToSession(liveId(session), session.getId(),
                    new TextMessage(objectMapper.writeValueAsString(frame)));
        } catch (JsonProcessingException exception) {
            log.error("Live chat error serialization failed", exception);
        }
    }

    private boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private Long userId(WebSocketSession session) {
        return (Long) session.getAttributes().get(LiveChatWebSocketHandshakeInterceptor.USER_ID_ATTRIBUTE);
    }

    private Long liveId(WebSocketSession session) {
        return (Long) session.getAttributes().get(LiveChatWebSocketHandshakeInterceptor.LIVE_ID_ATTRIBUTE);
    }

    private String now() {
        return LocalDateTime.now().format(FORMATTER);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionRegistry.unregister(liveId(session), session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        sessionRegistry.unregister(liveId(session), session.getId());
        if (session.isOpen()) session.close(CloseStatus.SERVER_ERROR);
    }
}
