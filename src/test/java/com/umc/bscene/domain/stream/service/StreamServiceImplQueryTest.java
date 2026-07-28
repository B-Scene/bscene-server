package com.umc.bscene.domain.stream.service;

import com.umc.bscene.domain.chat.service.LiveChatRoomCloser;
import com.umc.bscene.domain.stream.dto.response.BandLiveHomeResponse;
import com.umc.bscene.domain.stream.dto.response.FanLiveHomeResponse;
import com.umc.bscene.domain.stream.dto.response.LiveAlarmToggleResponse;
import com.umc.bscene.domain.stream.dto.response.LiveHomeResponse;
import com.umc.bscene.domain.stream.dto.response.LiveStreamResponse;
import com.umc.bscene.domain.stream.dto.response.UpcomingLiveResponse;
import com.umc.bscene.domain.stream.entity.AudioStream;
import com.umc.bscene.domain.stream.entity.LiveAlarm;
import com.umc.bscene.domain.stream.enums.StreamStatus;
import com.umc.bscene.domain.stream.enums.code.error.StreamErrorCode;
import com.umc.bscene.domain.stream.exception.StreamException;
import com.umc.bscene.domain.stream.port.BandMemberPort;
import com.umc.bscene.domain.stream.port.FollowPort;
import com.umc.bscene.domain.stream.port.NotifyPort;
import com.umc.bscene.domain.stream.port.UserPort;
import com.umc.bscene.domain.stream.port.UserTermsPort;
import com.umc.bscene.domain.stream.repository.AudioStreamRepository;
import com.umc.bscene.domain.stream.repository.LiveAlarmRepository;
import com.umc.bscene.domain.stream.repository.ReportHistoryRepository;
import com.umc.bscene.domain.stream.repository.StreamMemberRepository;
import com.umc.bscene.domain.stream.repository.StreamReplayRepository;
import com.umc.bscene.domain.stream.sse.ViewerSsePresence;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.global.response.CursorPage;
import com.umc.bscene.global.security.util.JwtUtil;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * StreamServiceImpl 조회 계열(getLiveStreams / getFollowingLiveStreams / getLiveHome /
 * getUpcomingLives / toggleLiveAlarm) 단위 테스트.
 *
 * <p>동작뿐 아니라 "쿼리·포트 호출 모양"까지 고정한다. 성능 튜닝 시 N+1 배치 조회가 개별 조회로
 * 흩어지거나 단축 경로(short-circuit)가 사라지면 즉시 실패하도록 호출 횟수와 인자를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class StreamServiceImplQueryTest {

    private static final String HLS_URL = "https://hls.test";
    private static final String WEBRTC_URL = "https://webrtc.test";
    private static final String VIEWER_KEY_PREFIX = "viewer:";

    @Mock private JwtUtil jwtUtil;
    @Mock private AudioStreamRepository audioStreamRepository;
    @Mock private StreamMemberRepository streamMemberRepository;
    @Mock private LiveAlarmRepository liveAlarmRepository;
    @Mock private StreamReplayRepository streamReplayRepository;
    @Mock private ReportHistoryRepository reportHistoryRepository;
    @Mock private UserPort userPort;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private BandMemberPort bandMemberPort;
    @Mock private FollowPort followPort;
    @Mock private UserTermsPort userTermsPort;
    @Mock private NotifyPort notifyPort;
    @Mock private RestClient mtxRestClient;
    @Mock private ViewerSsePresence viewerSsePresence;
    @Mock private LiveChatRoomCloser liveChatRoomCloser;
    @Mock private DiscordMessageSender discordMessageSender;

    @Mock private ZSetOperations<String, String> zSetOperations;

    @Captor private ArgumentCaptor<Set<Long>> broadcasterIdsCaptor;
    @Captor private ArgumentCaptor<Collection<Long>> liveIdsCaptor;
    @Captor private ArgumentCaptor<List<String>> pathsCaptor;
    @Captor private ArgumentCaptor<Pageable> pageableCaptor;

    private StreamServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StreamServiceImpl(
                jwtUtil,
                audioStreamRepository,
                streamMemberRepository,
                liveAlarmRepository,
                streamReplayRepository,
                reportHistoryRepository,
                userPort,
                redisTemplate,
                bandMemberPort,
                followPort,
                userTermsPort,
                notifyPort,
                mtxRestClient,
                viewerSsePresence,
                liveChatRoomCloser,
                discordMessageSender,
                HLS_URL,
                WEBRTC_URL
        );
    }

    // ---------- 공통 헬퍼 ----------

    /** Redis SCAN이 돌려줄 "live:*" 키 목록을 세팅한다. */
    private void givenLiveKeys(String... keys) {
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(StreamFixtures.redisCursor(keys));
    }

    /*
     * 라이브 중인 세션이 하나도 없는 SCAN 결과.
     * StreamFixtures.redisCursor()는 next() 스텁까지 걸어두므로, 키가 0개면 그 스텁이 소비되지 않아
     * strict stubs의 UnnecessaryStubbingException에 걸린다. 그래서 빈 커서만 따로 만든다.
     */
    @SuppressWarnings("unchecked")
    private void givenNoLiveKeys() {
        Cursor<String> emptyCursor = mock(Cursor.class);
        when(emptyCursor.hasNext()).thenReturn(false);
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(emptyCursor);
    }

    /** viewerCountOf가 opsForZSet()을 타므로 페이지가 비어있지 않은 테스트에서는 항상 필요하다. */
    private void givenZSetOps() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    private void givenViewerCount(Long liveId, Long zCard) {
        when(zSetOperations.zCard(VIEWER_KEY_PREFIX + liveId)).thenReturn(zCard);
    }

    // ---------- getLiveStreams ----------

    @Nested
    @DisplayName("getLiveStreams - 현재 라이브 전체 목록")
    class GetLiveStreams {

        @Test
        @DisplayName("Redis에 라이브 키가 하나도 없으면 빈 페이지를 반환하고 DB·밴드 포트를 조회하지 않는다")
        void returnsEmptyPageWithoutQueryingWhenNoLiveKeys() {
            givenNoLiveKeys();

            CursorPage<LiveStreamResponse> page = service.getLiveStreams(null, 10);

            assertThat(page.getItems()).isEmpty();
            assertThat(page.getPageInfo().nextCursor()).isNull();
            assertThat(page.getPageInfo().hasNext()).isFalse();
            verifyNoInteractions(audioStreamRepository, bandMemberPort);
            verify(redisTemplate, never()).opsForZSet();
        }

        @Test
        @DisplayName("SCAN 키에서 'live:' prefix를 제거한 path로 size+1개를 조회한다")
        void stripsLiveKeyPrefixAndFetchesSizePlusOne() {
            givenLiveKeys("live:path-11", "live:path-12");
            when(audioStreamRepository.findLivePage(anyList(), isNull(), any(Pageable.class)))
                    .thenReturn(List.of(
                            StreamFixtures.stream(11L, 21L, 31L, StreamStatus.OPEN),
                            StreamFixtures.stream(12L, 22L, 32L, StreamStatus.OPEN)
                    ));
            when(bandMemberPort.getBandNameWithBandProfileByBroadcasterId(Set.of(21L, 22L)))
                    .thenReturn(List.of());
            givenZSetOps();

            service.getLiveStreams(null, 2);

            verify(audioStreamRepository).findLivePage(pathsCaptor.capture(), isNull(), pageableCaptor.capture());
            assertThat(pathsCaptor.getValue()).containsExactlyInAnyOrder("path-11", "path-12");
            assertThat(pageableCaptor.getValue()).isEqualTo(PageRequest.ofSize(3));
        }

        @Test
        @DisplayName("커서가 주어지면 그대로 조회 조건에 전달된다")
        void passesCursorThrough() {
            givenLiveKeys("live:path-11");
            when(audioStreamRepository.findLivePage(anyList(), eq(100L), any(Pageable.class)))
                    .thenReturn(List.of(StreamFixtures.stream(11L, 21L, 31L, StreamStatus.OPEN)));
            when(bandMemberPort.getBandNameWithBandProfileByBroadcasterId(Set.of(21L)))
                    .thenReturn(List.of());
            givenZSetOps();

            service.getLiveStreams(100L, 5);

            verify(audioStreamRepository).findLivePage(anyList(), eq(100L), eq(PageRequest.ofSize(6)));
        }

        @Test
        @DisplayName("N+1 방지: 페이지의 모든 broadcasterId를 담은 Set으로 밴드 정보를 정확히 한 번만 조회한다")
        void batchesBandInfoLookupIntoSingleCall() {
            givenLiveKeys("live:path-11", "live:path-12", "live:path-13");
            when(audioStreamRepository.findLivePage(anyList(), isNull(), any(Pageable.class)))
                    .thenReturn(List.of(
                            StreamFixtures.stream(11L, 21L, 31L, StreamStatus.OPEN),
                            StreamFixtures.stream(12L, 22L, 32L, StreamStatus.OPEN),
                            StreamFixtures.stream(13L, 23L, 33L, StreamStatus.OPEN)
                    ));
            when(bandMemberPort.getBandNameWithBandProfileByBroadcasterId(Set.of(21L, 22L, 23L)))
                    .thenReturn(List.of(
                            StreamFixtures.bandInfo(21L, "밴드21", "https://cdn.test/b21.jpg"),
                            StreamFixtures.bandInfo(22L, "밴드22", "https://cdn.test/b22.jpg"),
                            StreamFixtures.bandInfo(23L, "밴드23", "https://cdn.test/b23.jpg")
                    ));
            givenZSetOps();

            CursorPage<LiveStreamResponse> page = service.getLiveStreams(null, 10);

            verify(bandMemberPort, times(1))
                    .getBandNameWithBandProfileByBroadcasterId(broadcasterIdsCaptor.capture());
            assertThat(broadcasterIdsCaptor.getValue()).containsExactlyInAnyOrder(21L, 22L, 23L);
            assertThat(page.getItems()).extracting(LiveStreamResponse::bandName)
                    .containsExactly("밴드21", "밴드22", "밴드23");
        }

        @Test
        @DisplayName("N+1 방지: 같은 송출자의 라이브가 여러 개여도 Set으로 합쳐져 한 번만 조회한다")
        void collapsesDuplicateBroadcasterIdsIntoOneSetEntry() {
            givenLiveKeys("live:path-11", "live:path-12", "live:path-13");
            when(audioStreamRepository.findLivePage(anyList(), isNull(), any(Pageable.class)))
                    .thenReturn(List.of(
                            StreamFixtures.stream(11L, 21L, 31L, StreamStatus.OPEN),
                            StreamFixtures.stream(12L, 21L, 31L, StreamStatus.OPEN),
                            StreamFixtures.stream(13L, 21L, 31L, StreamStatus.OPEN)
                    ));
            when(bandMemberPort.getBandNameWithBandProfileByBroadcasterId(Set.of(21L)))
                    .thenReturn(List.of(StreamFixtures.bandInfo(21L, "밴드21", "https://cdn.test/b21.jpg")));
            givenZSetOps();

            service.getLiveStreams(null, 10);

            verify(bandMemberPort, times(1))
                    .getBandNameWithBandProfileByBroadcasterId(broadcasterIdsCaptor.capture());
            assertThat(broadcasterIdsCaptor.getValue()).containsExactly(21L);
        }

        @Test
        @DisplayName("조회 결과가 비면 밴드 정보 조회 없이 빈 페이지를 반환한다")
        void skipsBandLookupWhenPageIsEmpty() {
            givenLiveKeys("live:path-11");
            when(audioStreamRepository.findLivePage(anyList(), isNull(), any(Pageable.class)))
                    .thenReturn(List.of());

            CursorPage<LiveStreamResponse> page = service.getLiveStreams(null, 10);

            assertThat(page.getItems()).isEmpty();
            assertThat(page.getPageInfo().hasNext()).isFalse();
            assertThat(page.getPageInfo().nextCursor()).isNull();
            verifyNoInteractions(bandMemberPort);
            verify(redisTemplate, never()).opsForZSet();
        }

        @Test
        @DisplayName("조회 행이 size+1이면 size개로 잘리고 hasNext=true, nextCursor는 잘린 페이지의 마지막 id다")
        void trimsToSizeAndSetsNextCursorWhenHasNext() {
            givenLiveKeys("live:path-11");
            when(audioStreamRepository.findLivePage(anyList(), isNull(), any(Pageable.class)))
                    .thenReturn(List.of(
                            StreamFixtures.stream(13L, 21L, 31L, StreamStatus.OPEN),
                            StreamFixtures.stream(12L, 21L, 31L, StreamStatus.OPEN),
                            StreamFixtures.stream(11L, 21L, 31L, StreamStatus.OPEN)
                    ));
            when(bandMemberPort.getBandNameWithBandProfileByBroadcasterId(Set.of(21L)))
                    .thenReturn(List.of());
            givenZSetOps();

            CursorPage<LiveStreamResponse> page = service.getLiveStreams(null, 2);

            assertThat(page.getItems()).extracting(LiveStreamResponse::liveId).containsExactly(13L, 12L);
            assertThat(page.getPageInfo().hasNext()).isTrue();
            assertThat(page.getPageInfo().nextCursor()).isEqualTo(12L);
        }

        @Test
        @DisplayName("조회 행이 정확히 size면 경계에서 hasNext=false, nextCursor=null이다")
        void hasNextIsFalseAtExactSizeBoundary() {
            givenLiveKeys("live:path-11");
            when(audioStreamRepository.findLivePage(anyList(), isNull(), any(Pageable.class)))
                    .thenReturn(List.of(
                            StreamFixtures.stream(13L, 21L, 31L, StreamStatus.OPEN),
                            StreamFixtures.stream(12L, 21L, 31L, StreamStatus.OPEN)
                    ));
            when(bandMemberPort.getBandNameWithBandProfileByBroadcasterId(Set.of(21L)))
                    .thenReturn(List.of());
            givenZSetOps();

            CursorPage<LiveStreamResponse> page = service.getLiveStreams(null, 2);

            assertThat(page.getItems()).hasSize(2);
            assertThat(page.getPageInfo().hasNext()).isFalse();
            assertThat(page.getPageInfo().nextCursor()).isNull();
        }

        @Test
        @DisplayName("조회 행이 size 미만이면 hasNext=false다")
        void hasNextIsFalseWhenFewerRowsThanSize() {
            givenLiveKeys("live:path-11");
            when(audioStreamRepository.findLivePage(anyList(), isNull(), any(Pageable.class)))
                    .thenReturn(List.of(StreamFixtures.stream(11L, 21L, 31L, StreamStatus.OPEN)));
            when(bandMemberPort.getBandNameWithBandProfileByBroadcasterId(Set.of(21L)))
                    .thenReturn(List.of());
            givenZSetOps();

            CursorPage<LiveStreamResponse> page = service.getLiveStreams(null, 5);

            assertThat(page.getItems()).hasSize(1);
            assertThat(page.getPageInfo().hasNext()).isFalse();
            assertThat(page.getPageInfo().nextCursor()).isNull();
        }

        @Test
        @DisplayName("size=1이면 1개만 내려가고 nextCursor는 그 1개의 id다")
        void supportsSizeOne() {
            givenLiveKeys("live:path-11");
            when(audioStreamRepository.findLivePage(anyList(), isNull(), any(Pageable.class)))
                    .thenReturn(List.of(
                            StreamFixtures.stream(13L, 21L, 31L, StreamStatus.OPEN),
                            StreamFixtures.stream(12L, 21L, 31L, StreamStatus.OPEN)
                    ));
            when(bandMemberPort.getBandNameWithBandProfileByBroadcasterId(Set.of(21L)))
                    .thenReturn(List.of());
            givenZSetOps();

            CursorPage<LiveStreamResponse> page = service.getLiveStreams(null, 1);

            verify(audioStreamRepository).findLivePage(anyList(), isNull(), eq(PageRequest.ofSize(2)));
            assertThat(page.getItems()).extracting(LiveStreamResponse::liveId).containsExactly(13L);
            assertThat(page.getPageInfo().nextCursor()).isEqualTo(13L);
            assertThat(page.getPageInfo().hasNext()).isTrue();
        }

        @Test
        @DisplayName("밴드 정보가 없는 송출자는 NPE 없이 빈 문자열로 응답한다")
        void fallsBackToEmptyStringsWhenBandInfoMissing() {
            givenLiveKeys("live:path-11");
            when(audioStreamRepository.findLivePage(anyList(), isNull(), any(Pageable.class)))
                    .thenReturn(List.of(StreamFixtures.stream(11L, 21L, 31L, StreamStatus.OPEN)));
            when(bandMemberPort.getBandNameWithBandProfileByBroadcasterId(Set.of(21L)))
                    .thenReturn(List.of());
            givenZSetOps();
            givenViewerCount(11L, 4L);

            CursorPage<LiveStreamResponse> page = service.getLiveStreams(null, 10);

            assertThat(page.getItems()).singleElement()
                    .satisfies(item -> {
                        assertThat(item.bandName()).isEmpty();
                        assertThat(item.bandProfileImageUrl()).isEmpty();
                        assertThat(item.title()).isEqualTo("title-11");
                        assertThat(item.viewCount()).isEqualTo(4);
                    });
        }

        @Test
        @DisplayName("zCard가 null이면 시청자 수는 0으로 응답한다")
        void viewerCountIsZeroWhenZCardIsNull() {
            givenLiveKeys("live:path-11");
            when(audioStreamRepository.findLivePage(anyList(), isNull(), any(Pageable.class)))
                    .thenReturn(List.of(StreamFixtures.stream(11L, 21L, 31L, StreamStatus.OPEN)));
            when(bandMemberPort.getBandNameWithBandProfileByBroadcasterId(Set.of(21L)))
                    .thenReturn(List.of());
            givenZSetOps();
            givenViewerCount(11L, null);

            CursorPage<LiveStreamResponse> page = service.getLiveStreams(null, 10);

            assertThat(page.getItems()).singleElement()
                    .extracting(LiveStreamResponse::viewCount)
                    .isEqualTo(0);
        }
    }

    // ---------- getFollowingLiveStreams ----------

    @Nested
    @DisplayName("getFollowingLiveStreams - 팔로우한 밴드의 라이브 목록")
    class GetFollowingLiveStreams {

        @Test
        @DisplayName("밴드 모드로 요청하면 FAN_MODE_ONLY 예외가 발생하고 아무 조회도 하지 않는다")
        void rejectsBandMode() {
            User bandUser = StreamFixtures.bandUser(1L);

            assertThatThrownBy(() -> service.getFollowingLiveStreams(bandUser, null, 10))
                    .isInstanceOf(StreamException.class)
                    .extracting(e -> ((StreamException) e).getBaseResponseCode())
                    .isEqualTo(StreamErrorCode.FAN_MODE_ONLY);

            verifyNoInteractions(followPort, redisTemplate, audioStreamRepository, bandMemberPort);
        }

        @Test
        @DisplayName("현재 모드가 없으면 FAN_MODE_ONLY 예외가 발생한다")
        void rejectsNullMode() {
            User noModeUser = StreamFixtures.user(1L, null);

            assertThatThrownBy(() -> service.getFollowingLiveStreams(noModeUser, null, 10))
                    .isInstanceOf(StreamException.class)
                    .extracting(e -> ((StreamException) e).getBaseResponseCode())
                    .isEqualTo(StreamErrorCode.FAN_MODE_ONLY);

            verifyNoInteractions(followPort, redisTemplate, audioStreamRepository, bandMemberPort);
        }

        @Test
        @DisplayName("팔로우한 밴드가 없으면 Redis SCAN도 DB 조회도 하지 않고 빈 페이지를 반환한다")
        void returnsEmptyPageWithoutScanWhenNoFollowedBands() {
            User fanUser = StreamFixtures.fanUser(1L);
            when(followPort.getFollowingBandIds(1L)).thenReturn(List.of());

            CursorPage<LiveStreamResponse> page = service.getFollowingLiveStreams(fanUser, null, 10);

            assertThat(page.getItems()).isEmpty();
            assertThat(page.getPageInfo().hasNext()).isFalse();
            assertThat(page.getPageInfo().nextCursor()).isNull();
            verifyNoInteractions(redisTemplate, audioStreamRepository, bandMemberPort);
        }

        @Test
        @DisplayName("Redis에 라이브 키가 없으면 페이지 쿼리를 던지지 않고 빈 페이지를 반환한다")
        void returnsEmptyPageWithoutPageQueryWhenNoLiveKeys() {
            User fanUser = StreamFixtures.fanUser(1L);
            when(followPort.getFollowingBandIds(1L)).thenReturn(List.of(31L));
            givenNoLiveKeys();

            CursorPage<LiveStreamResponse> page = service.getFollowingLiveStreams(fanUser, null, 10);

            assertThat(page.getItems()).isEmpty();
            assertThat(page.getPageInfo().hasNext()).isFalse();
            verifyNoInteractions(audioStreamRepository, bandMemberPort);
            verify(redisTemplate, never()).opsForZSet();
        }

        @Test
        @DisplayName("path 목록과 팔로우 밴드 ID로 size+1개를 조회한다")
        void queriesByPathsAndBandIdsWithSizePlusOne() {
            User fanUser = StreamFixtures.fanUser(1L);
            when(followPort.getFollowingBandIds(1L)).thenReturn(List.of(31L, 32L));
            givenLiveKeys("live:path-11", "live:path-12");
            when(audioStreamRepository.findLivePageByBandIds(anyList(), any(), isNull(), any(Pageable.class)))
                    .thenReturn(List.of(
                            StreamFixtures.stream(12L, 22L, 32L, StreamStatus.OPEN),
                            StreamFixtures.stream(11L, 21L, 31L, StreamStatus.OPEN)
                    ));
            when(bandMemberPort.getBandNameWithBandProfileByBroadcasterId(Set.of(21L, 22L)))
                    .thenReturn(List.of());
            givenZSetOps();

            service.getFollowingLiveStreams(fanUser, null, 2);

            ArgumentCaptor<Collection<Long>> bandIdsCaptor = ArgumentCaptor.captor();
            verify(audioStreamRepository).findLivePageByBandIds(
                    pathsCaptor.capture(), bandIdsCaptor.capture(), isNull(), pageableCaptor.capture());
            assertThat(pathsCaptor.getValue()).containsExactlyInAnyOrder("path-11", "path-12");
            assertThat(bandIdsCaptor.getValue()).containsExactly(31L, 32L);
            assertThat(pageableCaptor.getValue()).isEqualTo(PageRequest.ofSize(3));
            verify(audioStreamRepository, never()).findLivePage(anyList(), any(), any(Pageable.class));
        }

        @Test
        @DisplayName("N+1 방지: 잘린 페이지의 broadcasterId Set으로만 밴드 정보를 한 번 조회한다")
        void batchesBandInfoLookupIntoSingleCall() {
            User fanUser = StreamFixtures.fanUser(1L);
            when(followPort.getFollowingBandIds(1L)).thenReturn(List.of(31L, 32L, 33L));
            givenLiveKeys("live:path-11");
            when(audioStreamRepository.findLivePageByBandIds(anyList(), any(), isNull(), any(Pageable.class)))
                    .thenReturn(List.of(
                            StreamFixtures.stream(13L, 23L, 33L, StreamStatus.OPEN),
                            StreamFixtures.stream(12L, 22L, 32L, StreamStatus.OPEN),
                            StreamFixtures.stream(11L, 21L, 31L, StreamStatus.OPEN)
                    ));
            when(bandMemberPort.getBandNameWithBandProfileByBroadcasterId(Set.of(22L, 23L)))
                    .thenReturn(List.of(StreamFixtures.bandInfo(23L, "밴드23", "https://cdn.test/b23.jpg")));
            givenZSetOps();

            CursorPage<LiveStreamResponse> page = service.getFollowingLiveStreams(fanUser, null, 2);

            verify(bandMemberPort, times(1))
                    .getBandNameWithBandProfileByBroadcasterId(broadcasterIdsCaptor.capture());
            assertThat(broadcasterIdsCaptor.getValue()).containsExactlyInAnyOrder(22L, 23L);
            assertThat(page.getItems()).extracting(LiveStreamResponse::bandName)
                    .containsExactly("밴드23", "");
            assertThat(page.getPageInfo().hasNext()).isTrue();
            assertThat(page.getPageInfo().nextCursor()).isEqualTo(12L);
        }
    }

    // ---------- getLiveHome (팬 모드) ----------

    @Nested
    @DisplayName("getLiveHome - 팬 모드")
    class GetLiveHomeFan {

        private final LocalDateTime scheduledAt51 = LocalDateTime.of(2026, 7, 11, 21, 0);
        private final LocalDateTime scheduledAt52 = LocalDateTime.of(2026, 12, 25, 9, 5);

        @Test
        @DisplayName("현재 모드가 없으면 FORBIDDEN_REQUEST 예외가 발생하고 아무 조회도 하지 않는다")
        void rejectsNullMode() {
            User noModeUser = StreamFixtures.user(1L, null);

            assertThatThrownBy(() -> service.getLiveHome(noModeUser))
                    .isInstanceOf(StreamException.class)
                    .extracting(e -> ((StreamException) e).getBaseResponseCode())
                    .isEqualTo(StreamErrorCode.FORBIDDEN_REQUEST);

            verifyNoInteractions(
                    redisTemplate, audioStreamRepository, streamReplayRepository,
                    liveAlarmRepository, bandMemberPort
            );
        }

        @Test
        @DisplayName("섹션별 상한(라이브 3 / 다시보기 8 / 예정 3)으로 조회하고, 밴드 정보는 섹션마다 한 번씩만 배치 조회한다")
        void queriesEachSectionWithItsLimitAndBatchesBandLookupPerSection() {
            User fanUser = StreamFixtures.fanUser(1L);
            AudioStream replaySource31 = StreamFixtures.stream(31L, 41L, 71L, StreamStatus.CLOSED);
            AudioStream replaySource32 = StreamFixtures.stream(32L, 42L, 72L, StreamStatus.CLOSED);

            givenLiveKeys("live:path-11", "live:path-12", "live:path-13");
            when(audioStreamRepository.findLivePage(anyList(), isNull(), any(Pageable.class)))
                    .thenReturn(List.of(
                            StreamFixtures.stream(11L, 21L, 61L, StreamStatus.OPEN),
                            StreamFixtures.stream(12L, 22L, 62L, StreamStatus.OPEN),
                            StreamFixtures.stream(13L, 23L, 63L, StreamStatus.OPEN)
                    ));
            when(bandMemberPort.getBandNameWithBandProfileByBroadcasterId(Set.of(21L, 22L, 23L)))
                    .thenReturn(List.of(StreamFixtures.bandInfo(21L, "밴드21", "https://cdn.test/b21.jpg")));

            when(streamReplayRepository.findLatestReplays(any(Pageable.class)))
                    .thenReturn(List.of(
                            StreamFixtures.replay(101L, replaySource31, "s3/31-0.mp4", 60, 7L),
                            StreamFixtures.replay(102L, replaySource32, "s3/32-0.mp4", 90, 0L)
                    ));
            when(bandMemberPort.getBandNameWithBandProfileByBroadcasterId(Set.of(41L, 42L)))
                    .thenReturn(List.of(StreamFixtures.bandInfo(41L, "밴드41", "https://cdn.test/b41.jpg")));

            when(audioStreamRepository.findByStatusAndScheduledAtAfterOrderByScheduledAtAscIdAsc(
                    eq(StreamStatus.SCHEDULED), any(LocalDateTime.class), any(Pageable.class)))
                    .thenReturn(List.of(
                            StreamFixtures.scheduledStream(51L, 61L, 81L, scheduledAt51),
                            StreamFixtures.scheduledStream(52L, 62L, 82L, scheduledAt52)
                    ));
            when(bandMemberPort.getBandNameWithBandProfileByBroadcasterId(Set.of(61L, 62L)))
                    .thenReturn(List.of(StreamFixtures.bandInfo(61L, "밴드61", "https://cdn.test/b61.jpg")));
            when(liveAlarmRepository.findAlarmedLiveIds(1L, List.of(51L, 52L))).thenReturn(List.of(52L));

            givenZSetOps();
            givenViewerCount(11L, 5L);
            givenViewerCount(12L, null);
            givenViewerCount(13L, 7L);

            LiveHomeResponse response = service.getLiveHome(fanUser);

            // 섹션별 상한
            verify(audioStreamRepository).findLivePage(anyList(), isNull(), eq(PageRequest.ofSize(3)));
            verify(streamReplayRepository).findLatestReplays(PageRequest.ofSize(8));
            verify(audioStreamRepository).findByStatusAndScheduledAtAfterOrderByScheduledAtAscIdAsc(
                    eq(StreamStatus.SCHEDULED), any(LocalDateTime.class), eq(PageRequest.ofSize(3)));

            // N+1 방지: 섹션마다 정확히 한 번, 각 호출은 해당 섹션의 broadcasterId 전체를 담은 Set
            verify(bandMemberPort, times(3))
                    .getBandNameWithBandProfileByBroadcasterId(broadcasterIdsCaptor.capture());
            assertThat(broadcasterIdsCaptor.getAllValues()).containsExactly(
                    Set.of(21L, 22L, 23L), Set.of(41L, 42L), Set.of(61L, 62L)
            );

            // N+1 방지: 알림 설정 여부도 예정 라이브 ID 전체를 담은 한 번의 배치 조회
            verify(liveAlarmRepository, times(1)).findAlarmedLiveIds(eq(1L), liveIdsCaptor.capture());
            assertThat(liveIdsCaptor.getValue()).containsExactly(51L, 52L);

            assertThat(response).isInstanceOf(FanLiveHomeResponse.class);
            FanLiveHomeResponse fanHome = (FanLiveHomeResponse) response;

            assertThat(fanHome.liveNow()).containsExactly(
                    new FanLiveHomeResponse.LiveNowItem(11L, "https://cdn.test/b21.jpg", "title-11", "밴드21", 5),
                    new FanLiveHomeResponse.LiveNowItem(12L, "", "title-12", "", 0),
                    new FanLiveHomeResponse.LiveNowItem(13L, "", "title-13", "", 7)
            );
            assertThat(fanHome.replays()).containsExactly(
                    new FanLiveHomeResponse.ReplayItem(31L, "https://cdn.test/thumb-31.jpg", "title-31", "밴드41", 7L),
                    new FanLiveHomeResponse.ReplayItem(32L, "https://cdn.test/thumb-32.jpg", "title-32", "", 0L)
            );
            assertThat(fanHome.scheduled()).containsExactly(
                    new FanLiveHomeResponse.ScheduledItem(51L, "title-51", "밴드61", "7.11. (토) 오후 9:00", false),
                    new FanLiveHomeResponse.ScheduledItem(52L, "title-52", "", "12.25. (금) 오전 9:05", true)
            );
        }

        @Test
        @DisplayName("Redis에 라이브 키가 없으면 라이브 목록 쿼리를 던지지 않고 liveNow가 비어 있다")
        void skipsLivePageQueryWhenNoLiveKeys() {
            User fanUser = StreamFixtures.fanUser(1L);
            givenNoLiveKeys();
            when(streamReplayRepository.findLatestReplays(any(Pageable.class))).thenReturn(List.of());
            when(audioStreamRepository.findByStatusAndScheduledAtAfterOrderByScheduledAtAscIdAsc(
                    eq(StreamStatus.SCHEDULED), any(LocalDateTime.class), any(Pageable.class)))
                    .thenReturn(List.of());

            FanLiveHomeResponse fanHome = (FanLiveHomeResponse) service.getLiveHome(fanUser);

            assertThat(fanHome.liveNow()).isEmpty();
            verify(audioStreamRepository, never()).findLivePage(anyList(), any(), any(Pageable.class));
            verify(redisTemplate, never()).opsForZSet();
        }

        @Test
        @DisplayName("모든 섹션이 비면 밴드 정보 조회와 알림 배치 조회를 하지 않는다")
        void skipsBandAndAlarmLookupWhenAllSectionsAreEmpty() {
            User fanUser = StreamFixtures.fanUser(1L);
            givenNoLiveKeys();
            when(streamReplayRepository.findLatestReplays(any(Pageable.class))).thenReturn(List.of());
            when(audioStreamRepository.findByStatusAndScheduledAtAfterOrderByScheduledAtAscIdAsc(
                    eq(StreamStatus.SCHEDULED), any(LocalDateTime.class), any(Pageable.class)))
                    .thenReturn(List.of());

            FanLiveHomeResponse fanHome = (FanLiveHomeResponse) service.getLiveHome(fanUser);

            assertThat(fanHome.liveNow()).isEmpty();
            assertThat(fanHome.replays()).isEmpty();
            assertThat(fanHome.scheduled()).isEmpty();
            verifyNoInteractions(bandMemberPort, liveAlarmRepository);
        }

        @Test
        @DisplayName("예정 라이브의 scheduledAt이 null이면 포맷 결과도 null이다")
        void formatsNullScheduledAtAsNull() {
            User fanUser = StreamFixtures.fanUser(1L);
            givenNoLiveKeys();
            when(streamReplayRepository.findLatestReplays(any(Pageable.class))).thenReturn(List.of());
            when(audioStreamRepository.findByStatusAndScheduledAtAfterOrderByScheduledAtAscIdAsc(
                    eq(StreamStatus.SCHEDULED), any(LocalDateTime.class), any(Pageable.class)))
                    .thenReturn(List.of(StreamFixtures.scheduledStream(51L, 61L, 81L, null)));
            when(bandMemberPort.getBandNameWithBandProfileByBroadcasterId(Set.of(61L))).thenReturn(List.of());
            when(liveAlarmRepository.findAlarmedLiveIds(1L, List.of(51L))).thenReturn(List.of());

            FanLiveHomeResponse fanHome = (FanLiveHomeResponse) service.getLiveHome(fanUser);

            assertThat(fanHome.scheduled()).singleElement()
                    .satisfies(item -> {
                        assertThat(item.scheduledAt()).isNull();
                        assertThat(item.bandName()).isEmpty();
                        assertThat(item.notificationEnabled()).isFalse();
                    });
        }
    }

    // ---------- getLiveHome (밴드 모드) ----------

    @Nested
    @DisplayName("getLiveHome - 밴드 모드")
    class GetLiveHomeBand {

        @Test
        @DisplayName("라이브 3개 / 예정 5개 상한으로 조회하고 isMine 플래그를 내려준다")
        void queriesWithBandLimitsAndMarksMyLives() {
            User bandUser = StreamFixtures.bandUser(1L);
            LocalDateTime scheduledAt = LocalDateTime.of(2026, 7, 11, 21, 0);

            givenLiveKeys("live:path-11", "live:path-12");
            when(audioStreamRepository.findLivePage(anyList(), isNull(), any(Pageable.class)))
                    .thenReturn(List.of(
                            StreamFixtures.stream(11L, 1L, 61L, StreamStatus.OPEN),
                            StreamFixtures.stream(12L, 22L, 62L, StreamStatus.OPEN)
                    ));
            when(bandMemberPort.getBandNameWithBandProfileByBroadcasterId(Set.of(1L, 22L)))
                    .thenReturn(List.of(StreamFixtures.bandInfo(1L, "내밴드", "https://cdn.test/b1.jpg")));

            when(audioStreamRepository.findByStatusAndScheduledAtAfterOrderByScheduledAtAscIdAsc(
                    eq(StreamStatus.SCHEDULED), any(LocalDateTime.class), any(Pageable.class)))
                    .thenReturn(List.of(
                            StreamFixtures.scheduledStream(51L, 1L, 61L, scheduledAt),
                            StreamFixtures.scheduledStream(52L, 62L, 82L, null)
                    ));
            when(bandMemberPort.getBandNameWithBandProfileByBroadcasterId(Set.of(1L, 62L)))
                    .thenReturn(List.of(StreamFixtures.bandInfo(1L, "내밴드", "https://cdn.test/b1.jpg")));

            givenZSetOps();
            givenViewerCount(11L, 3L);
            givenViewerCount(12L, null);

            LiveHomeResponse response = service.getLiveHome(bandUser);

            verify(audioStreamRepository).findLivePage(anyList(), isNull(), eq(PageRequest.ofSize(3)));
            verify(audioStreamRepository).findByStatusAndScheduledAtAfterOrderByScheduledAtAscIdAsc(
                    eq(StreamStatus.SCHEDULED), any(LocalDateTime.class), eq(PageRequest.ofSize(5)));

            // 밴드 홈은 다시보기/알림 설정 섹션이 없으므로 해당 저장소를 건드리지 않는다
            verifyNoInteractions(streamReplayRepository, liveAlarmRepository);

            // N+1 방지: 섹션마다 배치 조회 한 번씩
            verify(bandMemberPort, times(2))
                    .getBandNameWithBandProfileByBroadcasterId(broadcasterIdsCaptor.capture());
            assertThat(broadcasterIdsCaptor.getAllValues())
                    .containsExactly(Set.of(1L, 22L), Set.of(1L, 62L));

            assertThat(response).isInstanceOf(BandLiveHomeResponse.class);
            BandLiveHomeResponse bandHome = (BandLiveHomeResponse) response;

            assertThat(bandHome.liveNow()).containsExactly(
                    new BandLiveHomeResponse.LiveNowItem(11L, "https://cdn.test/b1.jpg", "내밴드", "title-11", 3, true),
                    new BandLiveHomeResponse.LiveNowItem(12L, "", "", "title-12", 0, false)
            );
            assertThat(bandHome.scheduled()).containsExactly(
                    new BandLiveHomeResponse.ScheduledItem(51L, "내밴드", "title-51", "7.11. (토) 오후 9:00", true),
                    new BandLiveHomeResponse.ScheduledItem(52L, "", "title-52", null, false)
            );
        }

        @Test
        @DisplayName("라이브 키와 예정 라이브가 모두 없으면 밴드 정보 조회 없이 빈 섹션을 반환한다")
        void returnsEmptySectionsWithoutBandLookup() {
            User bandUser = StreamFixtures.bandUser(1L);
            givenNoLiveKeys();
            when(audioStreamRepository.findByStatusAndScheduledAtAfterOrderByScheduledAtAscIdAsc(
                    eq(StreamStatus.SCHEDULED), any(LocalDateTime.class), any(Pageable.class)))
                    .thenReturn(List.of());

            BandLiveHomeResponse bandHome = (BandLiveHomeResponse) service.getLiveHome(bandUser);

            assertThat(bandHome.liveNow()).isEmpty();
            assertThat(bandHome.scheduled()).isEmpty();
            verifyNoInteractions(bandMemberPort, streamReplayRepository, liveAlarmRepository);
            verify(audioStreamRepository, never()).findLivePage(anyList(), any(), any(Pageable.class));
            verify(redisTemplate, never()).opsForZSet();
        }
    }

    // ---------- getUpcomingLives ----------

    @Nested
    @DisplayName("getUpcomingLives - 예정된 라이브 목록")
    class GetUpcomingLives {

        private final LocalDateTime scheduledAt51 = LocalDateTime.of(2026, 7, 11, 21, 0);
        private final LocalDateTime scheduledAt52 = LocalDateTime.of(2026, 12, 25, 9, 5);

        private void verifyNoPageQuery() {
            verify(audioStreamRepository, never()).findUpcomingPage(any(), any(), any(), any());
            verify(audioStreamRepository, never()).findUpcomingPageByBandIds(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("커서 라이브가 삭제되어 조회되지 않으면 페이지 쿼리 없이 빈 페이지를 반환한다")
        void returnsEmptyPageForDeletedCursor() {
            User fanUser = StreamFixtures.fanUser(1L);
            when(audioStreamRepository.findById(99L)).thenReturn(Optional.empty());

            CursorPage<UpcomingLiveResponse> page = service.getUpcomingLives(fanUser, false, 99L, 10);

            assertThat(page.getItems()).isEmpty();
            assertThat(page.getPageInfo().hasNext()).isFalse();
            assertThat(page.getPageInfo().nextCursor()).isNull();
            verifyNoPageQuery();
            verifyNoInteractions(followPort, bandMemberPort, liveAlarmRepository);
        }

        @Test
        @DisplayName("커서 라이브의 예약 시각이 사라졌으면 페이지 쿼리 없이 빈 페이지를 반환한다")
        void returnsEmptyPageForStaleCursorWithoutScheduledAt() {
            User fanUser = StreamFixtures.fanUser(1L);
            when(audioStreamRepository.findById(99L))
                    .thenReturn(Optional.of(StreamFixtures.scheduledStream(99L, 61L, 81L, null)));

            CursorPage<UpcomingLiveResponse> page = service.getUpcomingLives(fanUser, false, 99L, 10);

            assertThat(page.getItems()).isEmpty();
            assertThat(page.getPageInfo().hasNext()).isFalse();
            verifyNoPageQuery();
            verifyNoInteractions(followPort, bandMemberPort, liveAlarmRepository);
        }

        @Test
        @DisplayName("following=true인데 팔로우한 밴드가 없으면 페이지 쿼리 없이 빈 페이지를 반환한다")
        void returnsEmptyPageWhenFollowingWithNoBands() {
            User fanUser = StreamFixtures.fanUser(1L);
            when(followPort.getFollowingBandIds(1L)).thenReturn(List.of());

            CursorPage<UpcomingLiveResponse> page = service.getUpcomingLives(fanUser, true, null, 10);

            assertThat(page.getItems()).isEmpty();
            assertThat(page.getPageInfo().hasNext()).isFalse();
            verifyNoPageQuery();
            verifyNoInteractions(bandMemberPort, liveAlarmRepository);
        }

        @Test
        @DisplayName("following=false면 전체 쿼리를 쓰고 팔로우 포트를 조회하지 않는다")
        void usesGlobalQueryWhenNotFollowing() {
            User fanUser = StreamFixtures.fanUser(1L);
            when(audioStreamRepository.findUpcomingPage(any(LocalDateTime.class), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(List.of());

            service.getUpcomingLives(fanUser, false, null, 10);

            verify(audioStreamRepository).findUpcomingPage(
                    any(LocalDateTime.class), isNull(), isNull(), eq(PageRequest.ofSize(11)));
            verify(audioStreamRepository, never()).findUpcomingPageByBandIds(any(), any(), any(), any(), any());
            verify(audioStreamRepository, never()).findById(any());
            verifyNoInteractions(followPort, bandMemberPort, liveAlarmRepository);
        }

        @Test
        @DisplayName("following=true면 팔로우 밴드 전용 쿼리를 쓴다")
        void usesFollowingQueryWhenFollowing() {
            User fanUser = StreamFixtures.fanUser(1L);
            when(followPort.getFollowingBandIds(1L)).thenReturn(List.of(81L, 82L));
            when(audioStreamRepository.findUpcomingPageByBandIds(
                    any(LocalDateTime.class), isNull(), isNull(), any(), any(Pageable.class)))
                    .thenReturn(List.of());

            service.getUpcomingLives(fanUser, true, null, 10);

            ArgumentCaptor<Collection<Long>> bandIdsCaptor = ArgumentCaptor.captor();
            verify(audioStreamRepository).findUpcomingPageByBandIds(
                    any(LocalDateTime.class), isNull(), isNull(), bandIdsCaptor.capture(), pageableCaptor.capture());
            assertThat(bandIdsCaptor.getValue()).containsExactly(81L, 82L);
            assertThat(pageableCaptor.getValue()).isEqualTo(PageRequest.ofSize(11));
            verify(audioStreamRepository, never()).findUpcomingPage(any(), any(), any(), any());
        }

        @Test
        @DisplayName("커서가 유효하면 커서 라이브의 예약 시각을 keyset 조건으로 함께 전달한다")
        void passesCursorScheduledAtIntoQuery() {
            User fanUser = StreamFixtures.fanUser(1L);
            when(audioStreamRepository.findById(51L))
                    .thenReturn(Optional.of(StreamFixtures.scheduledStream(51L, 61L, 81L, scheduledAt51)));
            when(audioStreamRepository.findUpcomingPage(
                    any(LocalDateTime.class), eq(scheduledAt51), eq(51L), any(Pageable.class)))
                    .thenReturn(List.of());

            service.getUpcomingLives(fanUser, false, 51L, 10);

            verify(audioStreamRepository).findUpcomingPage(
                    any(LocalDateTime.class), eq(scheduledAt51), eq(51L), eq(PageRequest.ofSize(11)));
        }

        @Test
        @DisplayName("팔로우 조회에서도 유효한 커서의 예약 시각이 keyset 조건으로 전달된다")
        void passesCursorScheduledAtIntoFollowingQuery() {
            User fanUser = StreamFixtures.fanUser(1L);
            when(followPort.getFollowingBandIds(1L)).thenReturn(List.of(81L));
            when(audioStreamRepository.findById(51L))
                    .thenReturn(Optional.of(StreamFixtures.scheduledStream(51L, 61L, 81L, scheduledAt51)));
            when(audioStreamRepository.findUpcomingPageByBandIds(
                    any(LocalDateTime.class), eq(scheduledAt51), eq(51L), any(), any(Pageable.class)))
                    .thenReturn(List.of());

            service.getUpcomingLives(fanUser, true, 51L, 10);

            verify(audioStreamRepository).findUpcomingPageByBandIds(
                    any(LocalDateTime.class), eq(scheduledAt51), eq(51L), eq(List.of(81L)), eq(PageRequest.ofSize(11)));
            verify(audioStreamRepository, never()).findUpcomingPage(any(), any(), any(), any());
        }

        @Test
        @DisplayName("조회 결과가 비면 밴드 정보와 알림 설정을 조회하지 않고 빈 페이지를 반환한다")
        void returnsEmptyPageWithoutEnrichmentWhenNoRows() {
            User fanUser = StreamFixtures.fanUser(1L);
            when(audioStreamRepository.findUpcomingPage(any(LocalDateTime.class), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(List.of());

            CursorPage<UpcomingLiveResponse> page = service.getUpcomingLives(fanUser, false, null, 10);

            assertThat(page.getItems()).isEmpty();
            assertThat(page.getPageInfo().hasNext()).isFalse();
            assertThat(page.getPageInfo().nextCursor()).isNull();
            verifyNoInteractions(bandMemberPort, liveAlarmRepository);
        }

        @Test
        @DisplayName("N+1 방지: 잘린 페이지 기준으로 밴드 정보와 알림 설정을 각각 한 번씩만 배치 조회한다")
        void batchesEnrichmentQueriesForTrimmedPage() {
            User fanUser = StreamFixtures.fanUser(1L);
            when(audioStreamRepository.findUpcomingPage(any(LocalDateTime.class), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(List.of(
                            StreamFixtures.scheduledStream(51L, 61L, 81L, scheduledAt51),
                            StreamFixtures.scheduledStream(52L, 62L, 82L, scheduledAt52),
                            StreamFixtures.scheduledStream(53L, 63L, 83L, scheduledAt52)
                    ));
            when(bandMemberPort.getBandNameWithBandProfileByBroadcasterId(Set.of(61L, 62L)))
                    .thenReturn(List.of(StreamFixtures.bandInfo(61L, "밴드61", "https://cdn.test/b61.jpg")));
            when(liveAlarmRepository.findAlarmedLiveIds(1L, List.of(51L, 52L))).thenReturn(List.of(51L));

            CursorPage<UpcomingLiveResponse> page = service.getUpcomingLives(fanUser, false, null, 2);

            verify(audioStreamRepository).findUpcomingPage(
                    any(LocalDateTime.class), isNull(), isNull(), eq(PageRequest.ofSize(3)));
            verify(bandMemberPort, times(1))
                    .getBandNameWithBandProfileByBroadcasterId(broadcasterIdsCaptor.capture());
            assertThat(broadcasterIdsCaptor.getValue()).containsExactlyInAnyOrder(61L, 62L);
            verify(liveAlarmRepository, times(1)).findAlarmedLiveIds(eq(1L), liveIdsCaptor.capture());
            assertThat(liveIdsCaptor.getValue()).containsExactly(51L, 52L);

            assertThat(page.getItems()).containsExactly(
                    new UpcomingLiveResponse(51L, "https://cdn.test/b61.jpg", "title-51", "밴드61", "2026-07-11 21:00:00", true),
                    new UpcomingLiveResponse(52L, "", "title-52", "", "2026-12-25 09:05:00", false)
            );
            assertThat(page.getPageInfo().hasNext()).isTrue();
            assertThat(page.getPageInfo().nextCursor()).isEqualTo(52L);
        }

        @Test
        @DisplayName("조회 행이 정확히 size면 hasNext=false, nextCursor=null이다")
        void hasNextIsFalseAtExactSizeBoundary() {
            User fanUser = StreamFixtures.fanUser(1L);
            when(audioStreamRepository.findUpcomingPage(any(LocalDateTime.class), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(List.of(
                            StreamFixtures.scheduledStream(51L, 61L, 81L, scheduledAt51),
                            StreamFixtures.scheduledStream(52L, 61L, 81L, scheduledAt52)
                    ));
            when(bandMemberPort.getBandNameWithBandProfileByBroadcasterId(Set.of(61L))).thenReturn(List.of());
            when(liveAlarmRepository.findAlarmedLiveIds(1L, List.of(51L, 52L))).thenReturn(List.of());

            CursorPage<UpcomingLiveResponse> page = service.getUpcomingLives(fanUser, false, null, 2);

            assertThat(page.getItems()).hasSize(2);
            assertThat(page.getPageInfo().hasNext()).isFalse();
            assertThat(page.getPageInfo().nextCursor()).isNull();
        }

        @Test
        @DisplayName("size=1이면 중복 송출자는 Set 한 항목으로 합쳐지고 1개만 내려간다")
        void collapsesDuplicateBroadcasterIdsWithSizeOne() {
            User fanUser = StreamFixtures.fanUser(1L);
            when(audioStreamRepository.findUpcomingPage(any(LocalDateTime.class), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(List.of(
                            StreamFixtures.scheduledStream(51L, 61L, 81L, scheduledAt51),
                            StreamFixtures.scheduledStream(52L, 61L, 81L, scheduledAt52)
                    ));
            when(bandMemberPort.getBandNameWithBandProfileByBroadcasterId(Set.of(61L))).thenReturn(List.of());
            when(liveAlarmRepository.findAlarmedLiveIds(1L, List.of(51L))).thenReturn(List.of());

            CursorPage<UpcomingLiveResponse> page = service.getUpcomingLives(fanUser, false, null, 1);

            verify(bandMemberPort, times(1))
                    .getBandNameWithBandProfileByBroadcasterId(broadcasterIdsCaptor.capture());
            assertThat(broadcasterIdsCaptor.getValue()).containsExactly(61L);
            assertThat(page.getItems()).extracting(UpcomingLiveResponse::liveId).containsExactly(51L);
            assertThat(page.getPageInfo().nextCursor()).isEqualTo(51L);
            assertThat(page.getPageInfo().hasNext()).isTrue();
        }

        @Test
        @DisplayName("같은 broadcasterId의 밴드 정보가 중복으로 오면 첫 번째 항목이 채택된다")
        void keepsFirstEntryOnBandInfoKeyCollision() {
            User fanUser = StreamFixtures.fanUser(1L);
            when(audioStreamRepository.findUpcomingPage(any(LocalDateTime.class), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(List.of(StreamFixtures.scheduledStream(51L, 61L, 81L, scheduledAt51)));
            when(bandMemberPort.getBandNameWithBandProfileByBroadcasterId(Set.of(61L)))
                    .thenReturn(List.of(
                            StreamFixtures.bandInfo(61L, "첫번째밴드", "https://cdn.test/first.jpg"),
                            StreamFixtures.bandInfo(61L, "두번째밴드", "https://cdn.test/second.jpg")
                    ));
            when(liveAlarmRepository.findAlarmedLiveIds(1L, List.of(51L))).thenReturn(List.of());

            CursorPage<UpcomingLiveResponse> page = service.getUpcomingLives(fanUser, false, null, 10);

            assertThat(page.getItems()).singleElement()
                    .satisfies(item -> {
                        assertThat(item.bandName()).isEqualTo("첫번째밴드");
                        assertThat(item.bandProfileImageUrl()).isEqualTo("https://cdn.test/first.jpg");
                    });
        }

        @Test
        @DisplayName("예약 시각이 null이면 포맷 결과도 null이고, 밴드 정보가 없으면 빈 문자열로 응답한다")
        void formatsNullScheduledAtAndMissingBandInfo() {
            User fanUser = StreamFixtures.fanUser(1L);
            when(audioStreamRepository.findUpcomingPage(any(LocalDateTime.class), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(List.of(StreamFixtures.scheduledStream(51L, 61L, 81L, null)));
            when(bandMemberPort.getBandNameWithBandProfileByBroadcasterId(Set.of(61L))).thenReturn(List.of());
            when(liveAlarmRepository.findAlarmedLiveIds(1L, List.of(51L))).thenReturn(List.of());

            CursorPage<UpcomingLiveResponse> page = service.getUpcomingLives(fanUser, false, null, 10);

            assertThat(page.getItems()).singleElement()
                    .satisfies(item -> {
                        assertThat(item.scheduledAt()).isNull();
                        assertThat(item.bandName()).isEmpty();
                        assertThat(item.bandProfileImageUrl()).isEmpty();
                        assertThat(item.isAlarmSet()).isFalse();
                    });
        }

        @Test
        @DisplayName("밴드 모드 유저도 예정 라이브를 조회할 수 있다")
        void allowsBandMode() {
            User bandUser = StreamFixtures.bandUser(1L);
            when(audioStreamRepository.findUpcomingPage(any(LocalDateTime.class), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(List.of());

            CursorPage<UpcomingLiveResponse> page = service.getUpcomingLives(bandUser, false, null, 10);

            assertThat(page.getItems()).isEmpty();
        }
    }

    // ---------- toggleLiveAlarm ----------

    @Nested
    @DisplayName("toggleLiveAlarm - 예정 라이브 알림 토글")
    class ToggleLiveAlarm {

        private final LocalDateTime scheduledAt = LocalDateTime.of(2026, 7, 11, 21, 0);

        @Test
        @DisplayName("라이브가 없으면 AUDIO_STREAM_NOT_FOUND 예외가 발생한다")
        void throwsWhenStreamNotFound() {
            User fanUser = StreamFixtures.fanUser(1L);
            when(audioStreamRepository.findById(51L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.toggleLiveAlarm(fanUser, 51L))
                    .isInstanceOf(StreamException.class)
                    .extracting(e -> ((StreamException) e).getBaseResponseCode())
                    .isEqualTo(StreamErrorCode.AUDIO_STREAM_NOT_FOUND);

            verifyNoInteractions(liveAlarmRepository);
        }

        @Test
        @DisplayName("SCHEDULED 상태가 아니면 ALARM_TARGET_NOT_SCHEDULED 예외가 발생한다")
        void throwsWhenStreamIsNotScheduled() {
            User fanUser = StreamFixtures.fanUser(1L);
            when(audioStreamRepository.findById(11L))
                    .thenReturn(Optional.of(StreamFixtures.stream(11L, 61L, 81L, StreamStatus.OPEN)));

            assertThatThrownBy(() -> service.toggleLiveAlarm(fanUser, 11L))
                    .isInstanceOf(StreamException.class)
                    .extracting(e -> ((StreamException) e).getBaseResponseCode())
                    .isEqualTo(StreamErrorCode.ALARM_TARGET_NOT_SCHEDULED);

            verifyNoInteractions(liveAlarmRepository);
        }

        @Test
        @DisplayName("SCHEDULED여도 예약 시각이 없으면 ALARM_TARGET_NOT_SCHEDULED 예외가 발생한다")
        void throwsWhenScheduledAtIsNull() {
            User fanUser = StreamFixtures.fanUser(1L);
            when(audioStreamRepository.findById(51L))
                    .thenReturn(Optional.of(StreamFixtures.scheduledStream(51L, 61L, 81L, null)));

            assertThatThrownBy(() -> service.toggleLiveAlarm(fanUser, 51L))
                    .isInstanceOf(StreamException.class)
                    .extracting(e -> ((StreamException) e).getBaseResponseCode())
                    .isEqualTo(StreamErrorCode.ALARM_TARGET_NOT_SCHEDULED);

            verifyNoInteractions(liveAlarmRepository);
        }

        @Test
        @DisplayName("이미 설정된 알림은 삭제하고 false를 반환한다")
        void deletesExistingAlarmAndReturnsFalse() {
            User fanUser = StreamFixtures.fanUser(1L);
            AudioStream stream = StreamFixtures.scheduledStream(51L, 61L, 81L, scheduledAt);
            LiveAlarm existing = LiveAlarm.builder().audioStream(stream).user(fanUser).build();

            when(audioStreamRepository.findById(51L)).thenReturn(Optional.of(stream));
            when(liveAlarmRepository.findByAudioStream_IdAndUser_Id(51L, 1L)).thenReturn(Optional.of(existing));

            LiveAlarmToggleResponse response = service.toggleLiveAlarm(fanUser, 51L);

            assertThat(response.alarmSet()).isFalse();
            verify(liveAlarmRepository).delete(existing);
            verify(liveAlarmRepository, never()).save(any(LiveAlarm.class));
        }

        @Test
        @DisplayName("설정된 알림이 없으면 저장하고 true를 반환한다")
        void savesNewAlarmAndReturnsTrue() {
            User fanUser = StreamFixtures.fanUser(1L);
            AudioStream stream = StreamFixtures.scheduledStream(51L, 61L, 81L, scheduledAt);

            when(audioStreamRepository.findById(51L)).thenReturn(Optional.of(stream));
            when(liveAlarmRepository.findByAudioStream_IdAndUser_Id(51L, 1L)).thenReturn(Optional.empty());

            LiveAlarmToggleResponse response = service.toggleLiveAlarm(fanUser, 51L);

            assertThat(response.alarmSet()).isTrue();
            ArgumentCaptor<LiveAlarm> savedCaptor = ArgumentCaptor.captor();
            verify(liveAlarmRepository).save(savedCaptor.capture());
            assertThat(savedCaptor.getValue().getAudioStream()).isSameAs(stream);
            assertThat(savedCaptor.getValue().getUser()).isSameAs(fanUser);
            verify(liveAlarmRepository, never()).delete(any(LiveAlarm.class));
        }

        @Test
        @DisplayName("동시 요청으로 unique 제약 위반이 나도 중복 설정으로 보고 true를 반환한다")
        void swallowsDataIntegrityViolationOnSave() {
            User fanUser = StreamFixtures.fanUser(1L);
            AudioStream stream = StreamFixtures.scheduledStream(51L, 61L, 81L, scheduledAt);

            when(audioStreamRepository.findById(51L)).thenReturn(Optional.of(stream));
            when(liveAlarmRepository.findByAudioStream_IdAndUser_Id(51L, 1L)).thenReturn(Optional.empty());
            when(liveAlarmRepository.save(any(LiveAlarm.class)))
                    .thenThrow(new DataIntegrityViolationException("uk_live_alarm_user_stream"));

            LiveAlarmToggleResponse response = service.toggleLiveAlarm(fanUser, 51L);

            assertThat(response.alarmSet()).isTrue();
        }
    }
}
