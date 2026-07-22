package com.umc.bscene.domain.chat.service;

import com.umc.bscene.domain.chat.dto.response.LiveChatWebSocketTicketResponse;
import com.umc.bscene.domain.chat.exception.ChatException;
import com.umc.bscene.domain.chat.enums.code.error.ChatErrorCode;
import com.umc.bscene.domain.stream.enums.StreamStatus;
import com.umc.bscene.domain.stream.repository.AudioStreamRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

@Service
public class LiveChatWebSocketTicketService {
    private static final String KEY_PREFIX = "live-chat:ws:ticket:";
    private static final String SUBPROTOCOL = "live-chat.v1";
    private static final long TICKET_TTL_SECONDS = 30L;
    private static final int TICKET_BYTES = 32;

    private final StringRedisTemplate redisTemplate;
    private final AudioStreamRepository audioStreamRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public LiveChatWebSocketTicketService(
            StringRedisTemplate redisTemplate,
            AudioStreamRepository audioStreamRepository
    ) {
        this.redisTemplate = redisTemplate;
        this.audioStreamRepository = audioStreamRepository;
    }

    public LiveChatWebSocketTicketResponse issue(Long userId, Long liveId) {
        if (!audioStreamRepository.existsByIdAndStatus(liveId, StreamStatus.OPEN)) {
            throw new ChatException(ChatErrorCode.LIVE_NOT_OPEN);
        }

        String ticket = generateTicket();
        redisTemplate.opsForValue().set(
                KEY_PREFIX + ticket,
                userId + ":" + liveId,
                Duration.ofSeconds(TICKET_TTL_SECONDS)
        );
        return new LiveChatWebSocketTicketResponse(ticket, SUBPROTOCOL, TICKET_TTL_SECONDS);
    }

    public TicketPrincipal consume(String ticket) {
        if (ticket == null || ticket.isBlank()) return null;
        String key = KEY_PREFIX + ticket;
        String value = redisTemplate.opsForValue().getAndDelete(key);
        if (value == null) return null;

        String[] parts = value.split(":", 2);
        if (parts.length != 2) return null;
        try {
            return new TicketPrincipal(Long.valueOf(parts[0]), Long.valueOf(parts[1]));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String generateTicket() {
        byte[] bytes = new byte[TICKET_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record TicketPrincipal(Long userId, Long liveId) {
    }
}
