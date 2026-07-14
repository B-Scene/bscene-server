package com.umc.bscene.domain.chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.bscene.domain.chat.websocket.LiveChatWebSocketSessionRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LiveChatRoomCloserTest {
    @Mock
    private LiveChatWebSocketSessionRegistry sessionRegistry;

    @Test
    void sendsLiveEndedEventBeforeClosingRoom() throws Exception {
        LiveChatRoomCloser closer = new LiveChatRoomCloser(sessionRegistry);

        closer.close(10L);

        ArgumentCaptor<TextMessage> message = ArgumentCaptor.forClass(TextMessage.class);
        verify(sessionRegistry).closeRoom(org.mockito.ArgumentMatchers.eq(10L), message.capture());
        JsonNode frame = new ObjectMapper().readTree(message.getValue().getPayload());
        assertEquals("system.event", frame.get("type").asText());
        assertEquals("live-ended", frame.get("data").get("event").asText());
    }
}
