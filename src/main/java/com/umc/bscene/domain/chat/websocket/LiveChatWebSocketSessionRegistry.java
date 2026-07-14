package com.umc.bscene.domain.chat.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class LiveChatWebSocketSessionRegistry {
    private static final int SEND_TIME_LIMIT_MILLIS = 10_000;
    private static final int BUFFER_SIZE_LIMIT_BYTES = 64 * 1024;
    private final Map<Long, Map<String, WebSocketSession>> sessionsByLive = new ConcurrentHashMap<>();

    public void register(Long liveId, WebSocketSession session) {
        WebSocketSession safeSession = new ConcurrentWebSocketSessionDecorator(
                session, SEND_TIME_LIMIT_MILLIS, BUFFER_SIZE_LIMIT_BYTES);
        sessionsByLive.computeIfAbsent(liveId, ignored -> new ConcurrentHashMap<>())
                .put(session.getId(), safeSession);
    }

    public void unregister(Long liveId, String sessionId) {
        sessionsByLive.computeIfPresent(liveId, (ignored, sessions) -> {
            sessions.remove(sessionId);
            return sessions.isEmpty() ? null : sessions;
        });
    }

    public void broadcast(Long liveId, TextMessage message) {
        Map<String, WebSocketSession> sessions = sessionsByLive.get(liveId);
        if (sessions == null) return;
        sessions.forEach((sessionId, session) -> {
            if (!session.isOpen()) {
                unregister(liveId, sessionId);
                return;
            }
            try {
                session.sendMessage(message);
            } catch (Exception exception) {
                unregister(liveId, sessionId);
                log.warn("Live chat push failed: liveId={}, sessionId={}",
                        liveId, sessionId, exception);
            }
        });
    }

    public void sendToSession(Long liveId, String sessionId, TextMessage message) {
        Map<String, WebSocketSession> sessions = sessionsByLive.get(liveId);
        if (sessions == null) return;
        WebSocketSession session = sessions.get(sessionId);
        if (session == null || !session.isOpen()) return;
        try {
            session.sendMessage(message);
        } catch (Exception exception) {
            unregister(liveId, sessionId);
            log.warn("Live chat push failed: liveId={}, sessionId={}",
                    liveId, sessionId, exception);
        }
    }
}
