package com.umc.bscene.domain.chat.websocket;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LiveChatWebSocketSessionRegistryTest {
    @Test
    void sendsFinalEventAndClosesAllRoomSessions() throws Exception {
        LiveChatWebSocketSessionRegistry registry = new LiveChatWebSocketSessionRegistry();
        WebSocketSession first = openSession("session-1");
        WebSocketSession second = openSession("session-2");
        TextMessage finalMessage = new TextMessage("{\"type\":\"system.event\"}");
        registry.register(10L, 1L, first);
        registry.register(10L, 2L, second);

        registry.closeRoom(10L, finalMessage);

        verify(first).sendMessage(finalMessage);
        verify(first).close(CloseStatus.NORMAL);
        verify(second).sendMessage(finalMessage);
        verify(second).close(CloseStatus.NORMAL);
    }

    private WebSocketSession openSession(String id) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(true);
        return session;
    }
}
