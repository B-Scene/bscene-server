package com.umc.bscene.domain.chat.service;

import com.umc.bscene.domain.chat.dto.response.ChatWebSocketTicketResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatWebSocketTicketServiceTest {
    @Mock
    private StringRedisTemplate redisTemplate;

    private ChatWebSocketTicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new ChatWebSocketTicketService(redisTemplate);
    }

    @Test
    void issuesUrlSafeRandomTicketWithThirtySecondTtl() {
        ChatWebSocketTicketResponse response = ticketService.issue(1L);

        assertEquals("dm.v1", response.subprotocol());
        assertEquals(30L, response.expiresIn());
        assertTrue(response.ticket().matches("[A-Za-z0-9_-]{43}"));
    }

    @Test
    void issuesDifferentTicketForEveryRequest() {
        ChatWebSocketTicketResponse first = ticketService.issue(1L);
        ChatWebSocketTicketResponse second = ticketService.issue(1L);

        assertNotEquals(first.ticket(), second.ticket());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void issuingNewTicketUsesSameUserKeyAndReplacesPreviousTicketAtomically() {
        ChatWebSocketTicketResponse first = ticketService.issue(1L);
        ChatWebSocketTicketResponse second = ticketService.issue(1L);

        ArgumentCaptor<List> keysCaptor = ArgumentCaptor.forClass(List.class);
        verify(redisTemplate, times(2)).execute(
                any(RedisScript.class),
                keysCaptor.capture(),
                any(Object[].class)
        );
        List<List> issuedKeys = keysCaptor.getAllValues();
        assertEquals("chat:ws:user-ticket:1", issuedKeys.get(0).get(0));
        assertEquals("chat:ws:user-ticket:1", issuedKeys.get(1).get(0));
        assertEquals(
                "chat:ws:ticket:" + first.ticket(),
                issuedKeys.get(0).get(1)
        );
        assertEquals(
                "chat:ws:ticket:" + second.ticket(),
                issuedKeys.get(1).get(1)
        );
        assertNotEquals(issuedKeys.get(0).get(1), issuedKeys.get(1).get(1));
    }

    @Test
    void rejectsBlankTicketWithoutAccessingRedis() {
        assertNull(ticketService.consume(" "));
        verifyNoInteractions(redisTemplate);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void returnsUserIdStoredForTicket() {
        when(redisTemplate.execute(
                any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn("7");

        assertEquals(7L, ticketService.consume("valid-ticket"));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void rejectsCorruptedNonNumericUserId() {
        when(redisTemplate.execute(
                any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn("not-a-number");

        assertNull(ticketService.consume("valid-ticket"));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void rejectsExpiredOrAlreadyConsumedTicket() {
        when(redisTemplate.execute(
                any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(null);

        assertNull(ticketService.consume("expired-ticket"));
    }
}
