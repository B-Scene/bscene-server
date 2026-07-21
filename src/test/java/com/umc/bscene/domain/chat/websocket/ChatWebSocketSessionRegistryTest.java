package com.umc.bscene.domain.chat.websocket;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatWebSocketSessionRegistryTest {
    private final ChatWebSocketSessionRegistry registry =
            new ChatWebSocketSessionRegistry();

    @Test
    void keepsMultipleOpenSessionsForSameUser() {
        WebSocketSession first = session("session-1", true);
        WebSocketSession second = session("session-2", true);

        registry.register(1L, first);
        registry.register(1L, second);

        assertEquals(2, registry.getOpenSessions(1L).size());
        assertTrue(registry.isOnline(1L));
    }

    @Test
    void unregisteringOneSessionKeepsOtherSessionOnline() {
        WebSocketSession first = session("session-1", true);
        WebSocketSession second = session("session-2", true);
        registry.register(1L, first);
        registry.register(1L, second);

        registry.unregister(1L, "session-1");

        assertEquals(1, registry.getOpenSessions(1L).size());
        assertTrue(registry.isOnline(1L));
    }

    @Test
    void removesClosedSessionWhenOpenSessionsAreRequested() {
        WebSocketSession closed = session("session-1", false);
        registry.register(1L, closed);

        assertTrue(registry.getOpenSessions(1L).isEmpty());
        assertFalse(registry.isOnline(1L));
    }

    private WebSocketSession session(String id, boolean open) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(open);
        return session;
    }
}
