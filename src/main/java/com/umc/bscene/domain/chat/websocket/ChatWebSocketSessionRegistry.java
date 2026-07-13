package com.umc.bscene.domain.chat.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
}
