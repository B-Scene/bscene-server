package com.umc.bscene.domain.chat.service;

import com.umc.bscene.domain.chat.enums.code.error.ChatErrorCode;
import com.umc.bscene.domain.chat.exception.ChatException;
import com.umc.bscene.domain.stream.enums.StreamStatus;
import com.umc.bscene.domain.stream.repository.AudioStreamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LiveChatWebSocketTicketServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private AudioStreamRepository audioStreamRepository;

    private LiveChatWebSocketTicketService service;

    @BeforeEach
    void setUp() {
        service = new LiveChatWebSocketTicketService(
                redisTemplate,
                audioStreamRepository
        );
    }

    @Test
    @DisplayName("진행 중인 라이브의 WebSocket 입장 티켓을 30초 동안 발급한다")
    void issueTicketSuccess() {
        when(audioStreamRepository.existsByIdAndStatus(10L, StreamStatus.OPEN))
                .thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        var response = service.issue(1L, 10L);

        assertThat(response.subprotocol()).isEqualTo("live-chat.v1");
        assertThat(response.expiresInSeconds()).isEqualTo(30L);
        assertThat(response.ticket()).matches("[A-Za-z0-9_-]{43}");
        verify(valueOperations).set(
                "live-chat:ws:ticket:" + response.ticket(),
                "1:10",
                Duration.ofSeconds(30)
        );
    }

    @Test
    @DisplayName("진행 중이 아닌 라이브에는 입장 티켓을 발급하지 않는다")
    void issueTicketFailsWhenLiveNotOpen() {
        when(audioStreamRepository.existsByIdAndStatus(10L, StreamStatus.OPEN))
                .thenReturn(false);

        assertThatThrownBy(() -> service.issue(1L, 10L))
                .isInstanceOf(ChatException.class)
                .extracting("baseResponseCode")
                .isEqualTo(ChatErrorCode.LIVE_NOT_OPEN);

        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("유효한 티켓을 한 번 소비하면 사용자와 라이브 식별자를 반환한다")
    void consumeTicketSuccess() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete("live-chat:ws:ticket:valid-ticket"))
                .thenReturn("1:10");

        var principal = service.consume("valid-ticket");

        assertThat(principal.userId()).isEqualTo(1L);
        assertThat(principal.liveId()).isEqualTo(10L);
        verify(valueOperations).getAndDelete(
                "live-chat:ws:ticket:valid-ticket"
        );
    }

    @Test
    @DisplayName("빈 티켓은 Redis 조회 없이 거부한다")
    void consumeRejectsBlankTicket() {
        assertThat(service.consume(" ")).isNull();

        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("만료되었거나 이미 소비한 티켓은 거부한다")
    void consumeRejectsMissingTicket() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete(anyString())).thenReturn(null);

        assertThat(service.consume("expired-ticket")).isNull();
    }

    @Test
    @DisplayName("저장값 형식이 잘못된 티켓은 거부한다")
    void consumeRejectsCorruptedTicket() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete(anyString()))
                .thenReturn("not-a-user:not-a-live");

        assertThat(service.consume("corrupted-ticket")).isNull();
    }
}
