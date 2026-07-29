package com.umc.bscene.domain.stream.scheduler;

import com.umc.bscene.domain.stream.entity.AudioStream;
import com.umc.bscene.domain.stream.enums.StreamStatus;
import com.umc.bscene.domain.stream.repository.AudioStreamRepository;
import com.umc.bscene.support.StreamFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 방치된 라이브 정리 스케줄러 검증.
 * <p>
 * 핵심 불변식:
 * - 유예 시간(30초)이 지난 건만 정리 대상이다
 * - Redis에 라이브 키가 살아 있는 방은 절대 종료시키지 않는다 (송출 중 오탐 방지)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StreamCleanupScheduler")
class StreamCleanupSchedulerTest {

    private static final Duration GRACE = Duration.ofSeconds(30);

    @Mock
    private AudioStreamRepository audioStreamRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @Captor
    private ArgumentCaptor<LocalDateTime> thresholdCaptor;

    @Captor
    private ArgumentCaptor<LocalDateTime> nowCaptor;

    private StreamCleanupScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new StreamCleanupScheduler(audioStreamRepository, redisTemplate);
    }

    @Nested
    @DisplayName("cancelAbandonedScheduled()")
    class CancelAbandonedScheduled {

        @Test
        @DisplayName("현재 시각과 30초 유예를 뺀 임계 시각을 벌크 UPDATE에 넘긴다")
        void passesNowAndThirtySecondThreshold() {
            LocalDateTime before = LocalDateTime.now();

            scheduler.cancelAbandonedScheduled();

            LocalDateTime after = LocalDateTime.now();
            verify(audioStreamRepository).cancelAbandonedScheduled(thresholdCaptor.capture(), nowCaptor.capture());

            LocalDateTime threshold = thresholdCaptor.getValue();
            LocalDateTime now = nowCaptor.getValue();
            assertThat(Duration.between(threshold, now)).isEqualTo(GRACE);
            assertThat(now).isBetween(before, after);
            assertThat(threshold).isBetween(before.minus(GRACE), after.minus(GRACE));
        }

        @Test
        @DisplayName("Redis는 건드리지 않는다 (DB 벌크 UPDATE 단독 경로)")
        void doesNotTouchRedis() {
            scheduler.cancelAbandonedScheduled();

            verifyNoInteractions(redisTemplate);
        }
    }

    @Nested
    @DisplayName("closeAbandonedOpen()")
    class CloseAbandonedOpen {

        @Test
        @DisplayName("OPEN 상태이면서 30초 이전에 시작된 라이브만 조회한다")
        void queriesOpenStreamsStartedBeforeGrace() {
            when(audioStreamRepository.findByStatusAndStartedAtBefore(eq(StreamStatus.OPEN), any(LocalDateTime.class)))
                    .thenReturn(List.of());
            LocalDateTime before = LocalDateTime.now();

            scheduler.closeAbandonedOpen();

            LocalDateTime after = LocalDateTime.now();
            verify(audioStreamRepository).findByStatusAndStartedAtBefore(eq(StreamStatus.OPEN), thresholdCaptor.capture());
            assertThat(thresholdCaptor.getValue()).isBetween(before.minus(GRACE), after.minus(GRACE));
        }

        @Test
        @DisplayName("대상이 없으면 Redis를 전혀 조회하지 않는다")
        void doesNotTouchRedisWhenNoCandidates() {
            when(audioStreamRepository.findByStatusAndStartedAtBefore(eq(StreamStatus.OPEN), any(LocalDateTime.class)))
                    .thenReturn(List.of());

            scheduler.closeAbandonedOpen();

            verifyNoInteractions(redisTemplate);
        }

        @Test
        @DisplayName("live 키가 살아 있으면 종료하지 않는다")
        void keepsStreamAliveWhenLiveKeyExists() {
            AudioStream live = StreamFixtures.stream(1L, 10L, 100L, StreamStatus.OPEN);
            when(audioStreamRepository.findByStatusAndStartedAtBefore(eq(StreamStatus.OPEN), any(LocalDateTime.class)))
                    .thenReturn(List.of(live));
            when(redisTemplate.hasKey("live:path-1")).thenReturn(true);

            scheduler.closeAbandonedOpen();

            assertThat(live.getStatus()).isEqualTo(StreamStatus.OPEN);
            assertThat(live.getClosedAt()).isNull();
            assertThat(live.getClosedViewerCount()).isNull();
            verify(redisTemplate, never()).opsForZSet();
        }

        @Test
        @DisplayName("live 키가 없으면 viewer ZSet 크기를 스냅샷으로 남기고 종료한다")
        void closesStreamWithViewerCountWhenLiveKeyMissing() {
            AudioStream dead = StreamFixtures.stream(2L, 11L, 101L, StreamStatus.OPEN);
            when(audioStreamRepository.findByStatusAndStartedAtBefore(eq(StreamStatus.OPEN), any(LocalDateTime.class)))
                    .thenReturn(List.of(dead));
            when(redisTemplate.hasKey("live:path-2")).thenReturn(false);
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.zCard("viewer:2")).thenReturn(7L);

            scheduler.closeAbandonedOpen();

            assertThat(dead.getStatus()).isEqualTo(StreamStatus.CLOSED);
            assertThat(dead.getClosedViewerCount()).isEqualTo(7);
            assertThat(dead.getClosedAt()).isNotNull();
        }

        @Test
        @DisplayName("hasKey가 null이면 라이브가 아닌 것으로 간주해 종료한다")
        void treatsNullHasKeyAsNotLive() {
            AudioStream dead = StreamFixtures.stream(3L, 12L, 102L, StreamStatus.OPEN);
            when(audioStreamRepository.findByStatusAndStartedAtBefore(eq(StreamStatus.OPEN), any(LocalDateTime.class)))
                    .thenReturn(List.of(dead));
            when(redisTemplate.hasKey("live:path-3")).thenReturn(null);
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.zCard("viewer:3")).thenReturn(2L);

            scheduler.closeAbandonedOpen();

            assertThat(dead.getStatus()).isEqualTo(StreamStatus.CLOSED);
            assertThat(dead.getClosedViewerCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("zCard가 null이면 시청자 수를 0으로 저장한다")
        void storesZeroWhenZCardIsNull() {
            AudioStream dead = StreamFixtures.stream(4L, 13L, 103L, StreamStatus.OPEN);
            when(audioStreamRepository.findByStatusAndStartedAtBefore(eq(StreamStatus.OPEN), any(LocalDateTime.class)))
                    .thenReturn(List.of(dead));
            when(redisTemplate.hasKey("live:path-4")).thenReturn(false);
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.zCard("viewer:4")).thenReturn(null);

            scheduler.closeAbandonedOpen();

            assertThat(dead.getStatus()).isEqualTo(StreamStatus.CLOSED);
            assertThat(dead.getClosedViewerCount()).isNotNull();
            assertThat(dead.getClosedViewerCount()).isZero();
        }

        @Test
        @DisplayName("살아 있는 방과 죽은 방이 섞여 있으면 죽은 방만 종료한다")
        void closesOnlyDeadStreamInMixedBatch() {
            AudioStream live = StreamFixtures.stream(1L, 10L, 100L, StreamStatus.OPEN);
            AudioStream dead = StreamFixtures.stream(2L, 11L, 101L, StreamStatus.OPEN);
            when(audioStreamRepository.findByStatusAndStartedAtBefore(eq(StreamStatus.OPEN), any(LocalDateTime.class)))
                    .thenReturn(List.of(live, dead));
            when(redisTemplate.hasKey("live:path-1")).thenReturn(true);
            when(redisTemplate.hasKey("live:path-2")).thenReturn(false);
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.zCard("viewer:2")).thenReturn(5L);

            scheduler.closeAbandonedOpen();

            assertThat(live.getStatus()).isEqualTo(StreamStatus.OPEN);
            assertThat(live.getClosedAt()).isNull();
            assertThat(dead.getStatus()).isEqualTo(StreamStatus.CLOSED);
            assertThat(dead.getClosedViewerCount()).isEqualTo(5);
            verify(zSetOperations, never()).zCard("viewer:1");
        }
    }
}
