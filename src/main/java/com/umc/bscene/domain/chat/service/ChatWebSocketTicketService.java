package com.umc.bscene.domain.chat.service;

import com.umc.bscene.domain.chat.dto.response.ChatWebSocketTicketResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@Service
public class ChatWebSocketTicketService {
    private static final String TICKET_KEY_PREFIX = "chat:ws:ticket:";
    private static final String USER_TICKET_KEY_PREFIX = "chat:ws:user-ticket:";
    private static final String SUBPROTOCOL = "dm.v1";
    private static final long TICKET_TTL_SECONDS = 30L;
    private static final int TICKET_BYTES = 32;

    private static final DefaultRedisScript<Long> ISSUE_TICKET_SCRIPT = new DefaultRedisScript<>("""
            local oldTicketKey = redis.call('GET', KEYS[1])
            if oldTicketKey then
                redis.call('DEL', oldTicketKey)
            end
            redis.call('SET', KEYS[1], KEYS[2], 'EX', ARGV[2])
            redis.call('SET', KEYS[2], ARGV[1], 'EX', ARGV[2])
            return 1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    public ChatWebSocketTicketService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public ChatWebSocketTicketResponse issue(Long userId) {
        String ticket = generateTicket();
        String ticketKey = TICKET_KEY_PREFIX + ticket;
        String userTicketKey = USER_TICKET_KEY_PREFIX + userId;

        redisTemplate.execute(
                ISSUE_TICKET_SCRIPT,
                List.of(userTicketKey, ticketKey),
                userId.toString(),
                Long.toString(TICKET_TTL_SECONDS)
        );

        return new ChatWebSocketTicketResponse(ticket, SUBPROTOCOL, TICKET_TTL_SECONDS);
    }

    private String generateTicket() {
        byte[] bytes = new byte[TICKET_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
