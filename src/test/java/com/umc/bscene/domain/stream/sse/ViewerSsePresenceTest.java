package com.umc.bscene.domain.stream.sse;

import com.umc.bscene.domain.stream.dto.response.CoHostUpgradeEvent;
import com.umc.bscene.domain.stream.dto.response.StreamRoomResponse;
import com.umc.bscene.domain.stream.entity.AudioStream;
import com.umc.bscene.domain.stream.enums.StreamStatus;
import com.umc.bscene.domain.stream.enums.code.error.StreamErrorCode;
import com.umc.bscene.domain.stream.exception.StreamException;
import com.umc.bscene.domain.stream.repository.AudioStreamRepository;
import com.umc.bscene.support.StreamFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * ViewerSsePresence의 프레젠스(ZSet) 관리 검증.
 * <p>
 * 성능 튜닝(하트비트 주기 변경, SCAN count 조정, 파이프라이닝 도입 등) 시에도 유지돼야 할 불변식:
 * - 송출자는 시청자 수에 포함되지 않는다
 * - 보기 전용 구독은 프레젠스에 등록되지 않는다
 * - sweep은 실제로 제거된 방에만 브로드캐스트한다(불필요한 팬아웃 금지)
 * - stale 판정 기준은 하트비트 주기(15s)의 3배인 45초다
 */
@ExtendWith(MockitoExtension.class)
class ViewerSsePresenceTest {

    @Mock
    private ViewerSseRegistry registry;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private AudioStreamRepository audioStreamRepository;
    @Mock
    private ZSetOperations<String, String> zSetOperations;

    private ViewerSsePresence presence;

    private static final long LIVE_ID = 10L;
    private static final long BROADCASTER_ID = 1L;
    private static final long LISTENER_ID = 2L;
    private static final String VIEWER_KEY = "viewer:" + LIVE_ID;
    private static final String MEMBER_KEY = "live-member:" + LIVE_ID;

    @BeforeEach
    void setUp() {
        presence = new ViewerSsePresence(registry, redisTemplate, audioStreamRepository);
    }

    private AudioStream openStream() {
        return StreamFixtures.stream(LIVE_ID, BROADCASTER_ID, 100L, StreamStatus.OPEN);
    }

    @Nested
    @DisplayName("subscribe - 구독 자격 검증")
    class SubscribeGuards {

        @Test
        @DisplayName("존재하지 않는 라이브는 구독할 수 없다")
        void unknownLiveIsRejected() {
            when(audioStreamRepository.findById(LIVE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> presence.subscribe(LISTENER_ID, LIVE_ID, false))
                    .isInstanceOf(StreamException.class)
                    .extracting(e -> ((StreamException) e).getBaseResponseCode())
                    .isEqualTo(StreamErrorCode.AUDIO_STREAM_NOT_FOUND);

            verifyNoInteractions(registry);
        }

        @ParameterizedTest(name = "{0} 상태는 구독 불가")
        @EnumSource(value = StreamStatus.class, names = {"SCHEDULED", "CLOSED", "CANCELED"})
        @DisplayName("OPEN이 아닌 라이브는 구독할 수 없다")
        void nonOpenStreamIsRejected(StreamStatus status) {
            when(audioStreamRepository.findById(LIVE_ID))
                    .thenReturn(Optional.of(StreamFixtures.stream(LIVE_ID, BROADCASTER_ID, 100L, status)));

            assertThatThrownBy(() -> presence.subscribe(LISTENER_ID, LIVE_ID, false))
                    .isInstanceOf(StreamException.class)
                    .extracting(e -> ((StreamException) e).getBaseResponseCode())
                    .isEqualTo(StreamErrorCode.AUDIO_STREAM_NOT_LIVE);

            verifyNoInteractions(registry);
        }
    }

    @Nested
    @DisplayName("subscribe - 보기 전용")
    class WatchOnlySubscribe {

        @Test
        @DisplayName("보기 전용 구독은 프레젠스에 등록되지 않고 카운트만 받는다")
        void watchOnlyDoesNotJoinPresence() {
            SseEmitter expected = new SseEmitter(0L);
            when(audioStreamRepository.findById(LIVE_ID)).thenReturn(Optional.of(openStream()));
            when(registry.registerWatchOnly(LIVE_ID)).thenReturn(expected);
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.zCard(VIEWER_KEY)).thenReturn(5L);

            SseEmitter actual = presence.subscribe(LISTENER_ID, LIVE_ID, true);

            assertThat(actual).isSameAs(expected);
            // 프레젠스 ZSet에 추가하지 않는다
            verify(zSetOperations, never()).add(any(), any(), org.mockito.ArgumentMatchers.anyDouble());
            // 유저당 1연결 제한(register)도 적용하지 않는다
            verify(registry, never()).register(anyLong(), anyLong(), anyBoolean(), any());
            // 구독 직후 현재 카운트를 전송한다
            verify(registry).broadcast(LIVE_ID, 5L);
        }
    }

    @Nested
    @DisplayName("subscribe - 방 내부 구독")
    class RoomSubscribe {

        @Test
        @DisplayName("청취자는 프레젠스에 등록되고 시청자 수에 포함된다")
        void listenerJoinsPresenceAndIsCounted() {
            when(audioStreamRepository.findById(LIVE_ID)).thenReturn(Optional.of(openStream()));
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.zCard(VIEWER_KEY)).thenReturn(1L);
            when(registry.register(anyLong(), anyLong(), anyBoolean(), any())).thenReturn(new SseEmitter(0L));

            long before = Instant.now().getEpochSecond();
            presence.subscribe(LISTENER_ID, LIVE_ID, false);
            long after = Instant.now().getEpochSecond();

            ArgumentCaptor<Double> score = ArgumentCaptor.forClass(Double.class);
            verify(zSetOperations).add(eq(VIEWER_KEY), eq(String.valueOf(LISTENER_ID)), score.capture());
            assertThat(score.getValue())
                    .as("프레젠스 score는 현재 epoch 초")
                    .isBetween((double) before, (double) after);

            verify(registry).register(eq(LIVE_ID), eq(LISTENER_ID), eq(true), any());
            verify(registry).broadcast(LIVE_ID, 1L);
        }

        @Test
        @DisplayName("송출자는 카운트 업데이트만 받고 시청자 수에는 포함되지 않는다")
        void broadcasterIsNotCounted() {
            when(audioStreamRepository.findById(LIVE_ID)).thenReturn(Optional.of(openStream()));
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.zCard(VIEWER_KEY)).thenReturn(0L);
            when(registry.register(anyLong(), anyLong(), anyBoolean(), any())).thenReturn(new SseEmitter(0L));

            presence.subscribe(BROADCASTER_ID, LIVE_ID, false);

            verify(zSetOperations, never()).add(any(), any(), org.mockito.ArgumentMatchers.anyDouble());
            verify(registry).register(eq(LIVE_ID), eq(BROADCASTER_ID), eq(false), any());
            verify(registry).broadcast(LIVE_ID, 0L);
        }

        @Test
        @DisplayName("청취자의 마지막 연결이 끊기면 프레젠스에서 제거하고 카운트를 갱신한다")
        void lastConnectionOfListenerRemovesPresence() {
            when(audioStreamRepository.findById(LIVE_ID)).thenReturn(Optional.of(openStream()));
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.zCard(VIEWER_KEY)).thenReturn(1L, 0L);
            when(registry.register(anyLong(), anyLong(), anyBoolean(), any())).thenReturn(new SseEmitter(0L));

            presence.subscribe(LISTENER_ID, LIVE_ID, false);

            ArgumentCaptor<Runnable> onLastGone = ArgumentCaptor.forClass(Runnable.class);
            verify(registry).register(eq(LIVE_ID), eq(LISTENER_ID), eq(true), onLastGone.capture());

            onLastGone.getValue().run();

            verify(zSetOperations).remove(VIEWER_KEY, String.valueOf(LISTENER_ID));
            verify(registry).broadcast(LIVE_ID, 0L);
        }

        @Test
        @DisplayName("송출자의 연결이 끊겨도 프레젠스 제거 없이 카운트만 다시 알린다")
        void lastConnectionOfBroadcasterDoesNotTouchPresence() {
            when(audioStreamRepository.findById(LIVE_ID)).thenReturn(Optional.of(openStream()));
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.zCard(VIEWER_KEY)).thenReturn(3L);
            when(registry.register(anyLong(), anyLong(), anyBoolean(), any())).thenReturn(new SseEmitter(0L));

            presence.subscribe(BROADCASTER_ID, LIVE_ID, false);

            ArgumentCaptor<Runnable> onLastGone = ArgumentCaptor.forClass(Runnable.class);
            verify(registry).register(eq(LIVE_ID), eq(BROADCASTER_ID), eq(false), onLastGone.capture());

            onLastGone.getValue().run();

            verify(zSetOperations, never()).remove(any(), any(Object[].class));
            verify(registry, times(2)).broadcast(LIVE_ID, 3L);
        }
    }

    @Nested
    @DisplayName("broadcastCount")
    class BroadcastCount {

        @Test
        @DisplayName("zCard가 null이면 0으로 브로드캐스트한다")
        void nullCardinalityBecomesZero() {
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.zCard(VIEWER_KEY)).thenReturn(null);

            presence.broadcastCount(LIVE_ID);

            verify(registry).broadcast(LIVE_ID, 0L);
        }

        @Test
        @DisplayName("현재 시청자 수를 그대로 브로드캐스트한다")
        void currentCountIsBroadcast() {
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.zCard(VIEWER_KEY)).thenReturn(123L);

            presence.broadcastCount(LIVE_ID);

            verify(registry).broadcast(LIVE_ID, 123L);
        }
    }

    @Nested
    @DisplayName("heartbeat")
    class Heartbeat {

        private ViewerSseRegistry.AliveListener capturedOnAlive() {
            ArgumentCaptor<ViewerSseRegistry.AliveListener> onAlive =
                    ArgumentCaptor.forClass(ViewerSseRegistry.AliveListener.class);
            verify(registry).pingAndCollectAlive(onAlive.capture());
            return onAlive.getValue();
        }

        @Test
        @DisplayName("살아있는 시청자(counted=true)의 시청자 프레젠스 score를 현재 시각으로 갱신한다")
        void aliveViewersAreTouched() {
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

            presence.heartbeat();

            long before = Instant.now().getEpochSecond();
            capturedOnAlive().onAlive(LIVE_ID, LISTENER_ID, true);
            long after = Instant.now().getEpochSecond();

            ArgumentCaptor<Double> score = ArgumentCaptor.forClass(Double.class);
            verify(zSetOperations).add(eq(VIEWER_KEY), eq(String.valueOf(LISTENER_ID)), score.capture());
            assertThat(score.getValue()).isBetween((double) before, (double) after);
        }

        @Test
        @DisplayName("진행자 프레젠스에 등록된 유저는 counted와 무관하게 score가 갱신된다")
        void memberPresenceIsRefreshedForRegisteredPublisher() {
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.score(MEMBER_KEY, String.valueOf(BROADCASTER_ID))).thenReturn(100d);

            presence.heartbeat();

            long before = Instant.now().getEpochSecond();
            capturedOnAlive().onAlive(LIVE_ID, BROADCASTER_ID, false);
            long after = Instant.now().getEpochSecond();

            ArgumentCaptor<Double> score = ArgumentCaptor.forClass(Double.class);
            verify(zSetOperations).add(eq(MEMBER_KEY), eq(String.valueOf(BROADCASTER_ID)), score.capture());
            assertThat(score.getValue()).isBetween((double) before, (double) after);
            // 송출자(counted=false)는 시청자 프레젠스에는 등록되지 않는다
            verify(zSetOperations, never()).add(eq(VIEWER_KEY), any(), org.mockito.ArgumentMatchers.anyDouble());
        }

        @Test
        @DisplayName("진행자 프레젠스에 없는(퇴장했거나 청취자인) 유저는 멤버 키에 추가되지 않는다")
        void unregisteredUserIsNotAddedToMemberPresence() {
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.score(MEMBER_KEY, String.valueOf(LISTENER_ID))).thenReturn(null);

            presence.heartbeat();

            capturedOnAlive().onAlive(LIVE_ID, LISTENER_ID, false);

            verify(zSetOperations, never()).add(eq(MEMBER_KEY), any(), org.mockito.ArgumentMatchers.anyDouble());
        }

        @Test
        @DisplayName("살아있는 연결이 없으면 프레젠스를 건드리지 않는다")
        void noAliveViewersMeansNoWrite() {
            presence.heartbeat();

            verify(registry).pingAndCollectAlive(any());
            verifyNoInteractions(zSetOperations);
        }
    }

    @Nested
    @DisplayName("sweepStale - 유령 시청자 정리")
    class SweepStale {

        @Test
        @DisplayName("하트비트 주기의 3배(45초) 이전 score를 제거 대상으로 삼는다")
        void cutoffIsFortyFiveSecondsAgo() {
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(redisTemplate.scan(any(ScanOptions.class)))
                    .thenReturn(StreamFixtures.redisCursor(VIEWER_KEY));
            when(zSetOperations.removeRangeByScore(eq(VIEWER_KEY), eq(0d), org.mockito.ArgumentMatchers.anyDouble()))
                    .thenReturn(0L);

            long before = Instant.now().getEpochSecond();
            presence.sweepStale();
            long after = Instant.now().getEpochSecond();

            ArgumentCaptor<Double> cutoff = ArgumentCaptor.forClass(Double.class);
            verify(zSetOperations).removeRangeByScore(eq(VIEWER_KEY), eq(0d), cutoff.capture());
            assertThat(cutoff.getValue()).isBetween((double) (before - 45), (double) (after - 45));
        }

        @Test
        @DisplayName("실제로 제거된 방에만 카운트를 다시 브로드캐스트한다")
        void broadcastsOnlyForRoomsThatChanged() {
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(redisTemplate.scan(any(ScanOptions.class)))
                    .thenReturn(StreamFixtures.redisCursor("viewer:10", "viewer:20", "viewer:30"));
            when(zSetOperations.removeRangeByScore(eq("viewer:10"), eq(0d), org.mockito.ArgumentMatchers.anyDouble()))
                    .thenReturn(2L);
            when(zSetOperations.removeRangeByScore(eq("viewer:20"), eq(0d), org.mockito.ArgumentMatchers.anyDouble()))
                    .thenReturn(0L);
            when(zSetOperations.removeRangeByScore(eq("viewer:30"), eq(0d), org.mockito.ArgumentMatchers.anyDouble()))
                    .thenReturn(null);
            when(zSetOperations.zCard("viewer:10")).thenReturn(7L);

            presence.sweepStale();

            verify(registry).broadcast(10L, 7L);
            // 제거된 유령이 없는 방(0건 / null)에는 불필요한 팬아웃을 하지 않는다
            verify(registry, never()).broadcast(eq(20L), anyLong());
            verify(registry, never()).broadcast(eq(30L), anyLong());
        }

        @Test
        @DisplayName("키 접두사를 떼어낸 liveId로 브로드캐스트한다")
        void liveIdIsParsedFromKeySuffix() {
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(redisTemplate.scan(any(ScanOptions.class)))
                    .thenReturn(StreamFixtures.redisCursor("viewer:98765"));
            when(zSetOperations.removeRangeByScore(eq("viewer:98765"), eq(0d), org.mockito.ArgumentMatchers.anyDouble()))
                    .thenReturn(1L);
            when(zSetOperations.zCard("viewer:98765")).thenReturn(0L);

            presence.sweepStale();

            verify(registry).broadcast(98765L, 0L);
        }

        @Test
        @DisplayName("정리할 방이 없으면 아무 것도 브로드캐스트하지 않는다")
        void emptyScanDoesNothing() {
            when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(StreamFixtures.redisCursor());

            presence.sweepStale();

            verifyNoInteractions(registry);
        }

        @Test
        @DisplayName("SCAN 커서는 정리 후 닫힌다")
        void cursorIsClosed() {
            StreamFixtures.FakeCursor cursor = StreamFixtures.redisCursor();
            when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);

            presence.sweepStale();

            assertThat(cursor.isClosed()).isTrue();
        }
    }

    @Nested
    @DisplayName("sweepStaleMembers - 유령 진행자 정리")
    class SweepStaleMembers {

        @Test
        @DisplayName("하트비트 주기의 3배(45초) 이전 score의 진행자를 프레젠스에서 제거한다")
        void removesStalePublishersWithSameCutoff() {
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(redisTemplate.scan(any(ScanOptions.class)))
                    .thenReturn(StreamFixtures.redisCursor(MEMBER_KEY));

            long before = Instant.now().getEpochSecond();
            presence.sweepStaleMembers();
            long after = Instant.now().getEpochSecond();

            ArgumentCaptor<Double> cutoff = ArgumentCaptor.forClass(Double.class);
            verify(zSetOperations).removeRangeByScore(eq(MEMBER_KEY), eq(0d), cutoff.capture());
            assertThat(cutoff.getValue()).isBetween((double) (before - 45), (double) (after - 45));
        }

        @Test
        @DisplayName("멤버 목록은 SSE로 내려가지 않으므로 제거가 있어도 브로드캐스트하지 않는다")
        void neverBroadcasts() {
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(redisTemplate.scan(any(ScanOptions.class)))
                    .thenReturn(StreamFixtures.redisCursor(MEMBER_KEY));
            when(zSetOperations.removeRangeByScore(eq(MEMBER_KEY), eq(0d), org.mockito.ArgumentMatchers.anyDouble()))
                    .thenReturn(3L);

            presence.sweepStaleMembers();

            verifyNoInteractions(registry);
        }

        @Test
        @DisplayName("SCAN 커서는 정리 후 닫힌다")
        void cursorIsClosed() {
            StreamFixtures.FakeCursor cursor = StreamFixtures.redisCursor();
            when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);

            presence.sweepStaleMembers();

            assertThat(cursor.isClosed()).isTrue();
        }
    }

    @Nested
    @DisplayName("notifyCoPublisherJoined - 진행자 합류 알림")
    class NotifyCoPublisherJoined {

        @Test
        @DisplayName("registry의 타겟 전송(coPublisherJoined)으로 위임하고 브로드캐스트는 사용하지 않는다")
        void delegatesToTargetedSendOnly() {
            StreamRoomResponse.CoPublisher joined =
                    new StreamRoomResponse.CoPublisher(21L, "https://webrtc.test/path-1-m902/whep");

            presence.notifyCoPublisherJoined(LIVE_ID, List.of(1L, 3L), joined);

            verify(registry).sendToUsers(LIVE_ID, List.of(1L, 3L), "coPublisherJoined", joined);
            // [공격] 멤버 path가 담긴 payload가 브로드캐스트로 전 구독자(청취자 포함)에게 새면 안 된다
            verify(registry, never()).broadcast(anyLong(), anyLong());
            verifyNoInteractions(redisTemplate);
        }

        @Test
        @DisplayName("대상이 비어 있으면 registry를 호출하지 않는다")
        void emptyTargetsSkipsSend() {
            presence.notifyCoPublisherJoined(
                    LIVE_ID, List.of(), new StreamRoomResponse.CoPublisher(21L, "url"));

            verifyNoInteractions(registry);
        }
    }

    @Nested
    @DisplayName("공동 송출자 업그레이드 SSE")
    class CoHostUpgradeEvents {

        @Test
        @DisplayName("업그레이드 요청은 송출자에게만 타겟 전송(coHostUpgradeRequested)되고 브로드캐스트하지 않는다")
        void upgradeRequestedTargetsBroadcasterOnly() {
            CoHostUpgradeEvent event = new CoHostUpgradeEvent(20L, "닉네임");

            presence.notifyCoHostUpgradeRequested(LIVE_ID, BROADCASTER_ID, event);

            verify(registry).sendToUsers(LIVE_ID, List.of(BROADCASTER_ID), "coHostUpgradeRequested", event);
            // [공격] 요청자 정보가 방 전체(청취자 포함)에 브로드캐스트되면 안 된다
            verify(registry, never()).broadcast(anyLong(), anyLong());
        }

        @Test
        @DisplayName("업그레이드 수락은 요청자에게만 타겟 전송(coHostUpgradeAccepted)되고 브로드캐스트하지 않는다")
        void upgradeAcceptedTargetsRequesterOnly() {
            CoHostUpgradeEvent event = new CoHostUpgradeEvent(20L, "닉네임");

            presence.notifyCoHostUpgradeAccepted(LIVE_ID, 20L, event);

            verify(registry).sendToUsers(LIVE_ID, List.of(20L), "coHostUpgradeAccepted", event);
            verify(registry, never()).broadcast(anyLong(), anyLong());
        }
    }
}
