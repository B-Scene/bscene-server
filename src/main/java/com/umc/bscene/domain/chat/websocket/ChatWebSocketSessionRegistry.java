package com.umc.bscene.domain.chat.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ChatWebSocketSessionRegistry {
    private static final int SEND_TIME_LIMIT_MILLIS = 10_000;
    private static final int BUFFER_SIZE_LIMIT_BYTES = 64 * 1024;

    private final Map<Long, Map<String, WebSocketSession>> sessionsByUser =
            new ConcurrentHashMap<>();

    public void register(Long userId, WebSocketSession session) {
        WebSocketSession safeSession = new ConcurrentWebSocketSessionDecorator(
                session,
                SEND_TIME_LIMIT_MILLIS,
                BUFFER_SIZE_LIMIT_BYTES
        );
        sessionsByUser.computeIfAbsent(userId, ignored -> new ConcurrentHashMap<>())
                .put(session.getId(), safeSession);
    }

    public void unregister(Long userId, String sessionId) {
        sessionsByUser.computeIfPresent(userId, (ignored, sessions) -> {
            sessions.remove(sessionId);
            return sessions.isEmpty() ? null : sessions;
        });
    }

    public List<WebSocketSession> getOpenSessions(Long userId) {
        Map<String, WebSocketSession> sessions = sessionsByUser.get(userId);
        if (sessions == null) return List.of();

        return sessions.values().stream()
                .filter(WebSocketSession::isOpen)
                .toList();
    }

    public boolean isOnline(Long userId) {
        return !getOpenSessions(userId).isEmpty();
    }

    public void sendToUser(Long userId, TextMessage message) {
        for (WebSocketSession session : getOpenSessions(userId)) {
            try {
                session.sendMessage(message);
            } catch (Exception exception) {
                unregister(userId, session.getId());
                log.warn("Chat WebSocket push failed: userId={}, sessionId={}",
                        userId, session.getId(), exception);
            }
        }
    }

    public void sendToSession(Long userId, String sessionId, TextMessage message) {
        Map<String, WebSocketSession> sessions = sessionsByUser.get(userId);
        if (sessions == null) return;

        WebSocketSession session = sessions.get(sessionId);
        if (session == null || !session.isOpen()) return;

        try {
            session.sendMessage(message);
        } catch (Exception exception) {
            unregister(userId, sessionId);
            log.warn("Chat WebSocket push failed: userId={}, sessionId={}",
                    userId, sessionId, exception);
        }
    }
}
