package com.umc.bscene.domain.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.bscene.domain.chat.dto.response.ChatWebSocketPushFrame;
import com.umc.bscene.domain.chat.dto.response.ChatWebSocketSystemEventData;
import com.umc.bscene.domain.chat.websocket.LiveChatWebSocketSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class LiveChatRoomCloser {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final LiveChatWebSocketSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void close(Long liveId) {
        ChatWebSocketPushFrame frame = new ChatWebSocketPushFrame(
                "system.event",
                null,
                new ChatWebSocketSystemEventData("live-ended"),
                null,
                LocalDateTime.now().format(FORMATTER)
        );
        try {
            sessionRegistry.closeRoom(
                    liveId,
                    new TextMessage(objectMapper.writeValueAsString(frame))
            );
        } catch (JsonProcessingException exception) {
            log.error("Live chat end event serialization failed: liveId={}", liveId, exception);
        }
    }
}
