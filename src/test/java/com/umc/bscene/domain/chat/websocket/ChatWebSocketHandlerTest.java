package com.umc.bscene.domain.chat.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.bscene.domain.chat.dto.request.ChatMessageReadRequest;
import com.umc.bscene.domain.chat.dto.request.ChatMessageSendRequest;
import com.umc.bscene.domain.chat.dto.response.ChatMessagePushData;
import com.umc.bscene.domain.chat.dto.response.ChatMessageReadPushData;
import com.umc.bscene.domain.chat.dto.response.ChatMessageReadResult;
import com.umc.bscene.domain.chat.dto.response.ChatMessageSendResult;
import com.umc.bscene.domain.chat.service.ChatMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class ChatWebSocketHandlerTest {
    private static final Long USER_ID = 1L;
    private static final String SESSION_ID = "session-1";

    @Mock
    private ChatWebSocketSessionRegistry sessionRegistry;
    @Mock
    private ChatMessageService chatMessageService;
    @Mock
    private WebSocketSession session;

    private ChatWebSocketHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        handler = new ChatWebSocketHandler(sessionRegistry, chatMessageService);
        objectMapper = new ObjectMapper();
        when(session.getAttributes()).thenReturn(Map.of(
                ChatWebSocketHandshakeInterceptor.USER_ID_ATTRIBUTE, USER_ID));
        lenient().when(session.getId()).thenReturn(SESSION_ID);
    }

    @Test
    void sendsConnectedSystemEventAfterConnectionEstablished() throws Exception {
        handler.afterConnectionEstablished(session);

        verify(sessionRegistry).register(USER_ID, session);
        JsonNode frame = captureSentFrame();
        assertEquals("system.event", frame.get("type").asText());
        assertEquals("connected", frame.get("data").get("event").asText());
        assertNull(frame.get("clientMsgId").textValue());
    }

    @Test
    void respondsWithPongToPing() throws Exception {
        handler.handleTextMessage(session, new TextMessage("""
                {"type":"ping","data":{},"clientMsgId":null}
                """));

        JsonNode frame = captureSentFrame();
        assertEquals("pong", frame.get("type").asText());
        assertEquals(0, frame.get("data").size());
        assertNull(frame.get("clientMsgId").textValue());
    }

    @Test
    void respondsWithSystemErrorForUnsupportedType() throws Exception {
        handler.handleTextMessage(session, new TextMessage("""
                {"type":"dm.unknown","data":{},"clientMsgId":null}
                """));

        JsonNode frame = captureSentFrame();
        assertEquals("system.error", frame.get("type").asText());
        assertEquals("DM_UNSUPPORTED_TYPE", frame.get("data").get("code").asText());
    }

    @Test
    void pushesMessageToSenderAndRecipient() throws Exception {
        String clientMsgId = "550e8400-e29b-41d4-a716-446655440000";
        ChatMessagePushData messageData = new ChatMessagePushData(
                10L, 2L, USER_ID, "sender", null,
                "hello", null, "2026-07-14 03:00:00");
        when(chatMessageService.send(eq(USER_ID), any(ChatMessageSendRequest.class)))
                .thenReturn(new ChatMessageSendResult(2L, messageData));

        handler.handleTextMessage(session, new TextMessage("""
                {"type":"dm.send","data":{"chatRoomId":2,"content":"hello"},
                 "clientMsgId":"550e8400-e29b-41d4-a716-446655440000"}
                """));

        ArgumentCaptor<TextMessage> senderMessage = ArgumentCaptor.forClass(TextMessage.class);
        ArgumentCaptor<TextMessage> recipientMessage = ArgumentCaptor.forClass(TextMessage.class);
        verify(sessionRegistry).sendToUser(eq(USER_ID), senderMessage.capture());
        verify(sessionRegistry).sendToUser(eq(2L), recipientMessage.capture());

        JsonNode senderFrame = objectMapper.readTree(senderMessage.getValue().getPayload());
        JsonNode recipientFrame = objectMapper.readTree(recipientMessage.getValue().getPayload());
        assertEquals("dm.message", senderFrame.get("type").asText());
        assertEquals(clientMsgId, senderFrame.get("clientMsgId").asText());
        assertEquals(10L, senderFrame.get("data").get("chatMessageId").asLong());
        assertNull(recipientFrame.get("clientMsgId").textValue());
    }

    @Test
    void pushesUpdatedReadStateToBothParticipants() throws Exception {
        ChatMessageReadPushData readData = new ChatMessageReadPushData(
                2L, USER_ID, 10L, "2026-07-14 03:01:00");
        when(chatMessageService.markRead(eq(USER_ID), any(ChatMessageReadRequest.class)))
                .thenReturn(new ChatMessageReadResult(2L, readData, true));

        handler.handleTextMessage(session, new TextMessage("""
                {"type":"dm.read","data":{"chatRoomId":2,"lastReadMessageId":10},
                 "clientMsgId":null}
                """));

        ArgumentCaptor<TextMessage> readerMessage = ArgumentCaptor.forClass(TextMessage.class);
        ArgumentCaptor<TextMessage> counterpartMessage = ArgumentCaptor.forClass(TextMessage.class);
        verify(sessionRegistry).sendToUser(eq(USER_ID), readerMessage.capture());
        verify(sessionRegistry).sendToUser(eq(2L), counterpartMessage.capture());

        JsonNode readerFrame = objectMapper.readTree(readerMessage.getValue().getPayload());
        JsonNode counterpartFrame = objectMapper.readTree(
                counterpartMessage.getValue().getPayload());
        assertEquals("dm.read", readerFrame.get("type").asText());
        assertEquals(10L, readerFrame.get("data").get("lastReadMessageId").asLong());
        assertEquals(readerFrame.get("data"), counterpartFrame.get("data"));
    }

    private JsonNode captureSentFrame() throws Exception {
        ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(sessionRegistry).sendToSession(
                org.mockito.ArgumentMatchers.eq(USER_ID),
                org.mockito.ArgumentMatchers.eq(SESSION_ID),
                messageCaptor.capture());
        return objectMapper.readTree(messageCaptor.getValue().getPayload());
    }
}
