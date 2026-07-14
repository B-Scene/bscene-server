package com.umc.bscene.domain.chat.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.repository.UserRepository;
import com.umc.bscene.domain.user.repository.UserBlockRepository;
import com.umc.bscene.domain.stream.repository.ReportHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class LiveChatWebSocketHandlerTest {
    private static final Long USER_ID = 1L;
    private static final Long LIVE_ID = 10L;
    private static final String SESSION_ID = "live-session-1";

    @Mock LiveChatWebSocketSessionRegistry sessionRegistry;
    @Mock UserRepository userRepository;
    @Mock UserBlockRepository userBlockRepository;
    @Mock ReportHistoryRepository reportHistoryRepository;
    @Mock WebSocketSession session;
    @Mock User user;

    private LiveChatWebSocketHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        handler = new LiveChatWebSocketHandler(
                sessionRegistry, userRepository, userBlockRepository, reportHistoryRepository);
        when(session.getAttributes()).thenReturn(Map.of(
                LiveChatWebSocketHandshakeInterceptor.USER_ID_ATTRIBUTE, USER_ID,
                LiveChatWebSocketHandshakeInterceptor.LIVE_ID_ATTRIBUTE, LIVE_ID));
        lenient().when(session.getId()).thenReturn(SESSION_ID);
    }

    @Test
    void registersRoomSessionAndSendsConnectedEvent() throws Exception {
        handler.afterConnectionEstablished(session);

        verify(sessionRegistry).register(LIVE_ID, USER_ID, session);
        JsonNode frame = captureSessionFrame();
        assertEquals("system.event", frame.get("type").asText());
        assertEquals("connected", frame.get("data").get("event").asText());
    }

    @Test
    void broadcastsMessageToLiveRoom() throws Exception {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(user.getName()).thenReturn("최준우");
        when(userBlockRepository.findBlockedUserIdsRelatedTo(USER_ID)).thenReturn(Set.of(2L));
        when(reportHistoryRepository.findReporterIdsByLiveIdAndTargetUserId(LIVE_ID, USER_ID))
                .thenReturn(Set.of(3L));

        handler.handleTextMessage(session, new TextMessage("""
                {"type":"live-chat.send","data":{"content":"라이브 자주 해주세요!"},
                 "clientMsgId":"550e8400-e29b-41d4-a716-446655440000"}
                """));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(sessionRegistry).broadcastExcept(eq(LIVE_ID), eq(Set.of(2L, 3L)), captor.capture());
        JsonNode frame = objectMapper.readTree(captor.getValue().getPayload());
        assertEquals("live-chat.message", frame.get("type").asText());
        assertEquals("최준우", frame.get("data").get("senderName").asText());
        assertEquals("라이브 자주 해주세요!", frame.get("data").get("content").asText());
    }

    @Test
    void rejectsBlankContent() throws Exception {
        handler.handleTextMessage(session, new TextMessage("""
                {"type":"live-chat.send","data":{"content":"  "},
                 "clientMsgId":"550e8400-e29b-41d4-a716-446655440000"}
                """));

        JsonNode frame = captureSessionFrame();
        assertEquals("system.error", frame.get("type").asText());
        assertEquals("LIVE_CHAT_CONTENT_EMPTY", frame.get("data").get("code").asText());
    }

    private JsonNode captureSessionFrame() throws Exception {
        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(sessionRegistry).sendToSession(eq(LIVE_ID), eq(SESSION_ID), captor.capture());
        return objectMapper.readTree(captor.getValue().getPayload());
    }
}
