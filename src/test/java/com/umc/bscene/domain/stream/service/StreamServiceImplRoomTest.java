package com.umc.bscene.domain.stream.service;

import com.umc.bscene.domain.chat.service.LiveChatRoomCloser;
import com.umc.bscene.domain.stream.dto.request.StreamCreateRequest;
import com.umc.bscene.domain.stream.dto.response.BandSummaryResponse;
import com.umc.bscene.domain.stream.dto.response.MtxPathResponse;
import com.umc.bscene.domain.stream.dto.response.StreamCreateResponse;
import com.umc.bscene.domain.stream.dto.response.StreamRoomResponse;
import com.umc.bscene.domain.stream.dto.response.StreamSummaryResponse;
import com.umc.bscene.domain.stream.entity.AudioStream;
import com.umc.bscene.domain.stream.entity.mapper.StreamMember;
import com.umc.bscene.domain.stream.enums.StreamMemberStatus;
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
import com.umc.bscene.global.notification.message.PushMessage;
import com.umc.bscene.global.security.util.JwtUtil;
import com.umc.bscene.support.StreamFixtures;
import com.umc.bscene.support.TxSyncSupport;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * StreamServiceImpl의 방(Room) 관련 동작 단위 테스트.
 * 성능 튜닝 전에 동작뿐 아니라 호출 형태(쿼리 횟수, afterCommit 순서)까지 고정한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StreamServiceImpl 라이브 방 동작")
class StreamServiceImplRoomTest {

    private static final String HLS_URL = "https://hls.test";
    private static final String WEBRTC_URL = "https://webrtc.test";

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
    @Mock private ValueOperations<String, String> valueOperations;

    private RestClient.RequestBodyUriSpec mtxKickUriSpec;

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
        TxSyncSupport.begin();
    }

    @AfterEach
    void tearDown() {
        TxSyncSupport.end();
    }

    // --- 공용 헬퍼 ---

    private static void assertStreamError(ThrowingCallable callable, StreamErrorCode expected) {
        assertThatThrownBy(callable)
                .isInstanceOf(StreamException.class)
                .hasFieldOrPropertyWithValue("baseResponseCode", expected);
    }

    /** 유효한 access 토큰으로 userId가 추출되도록 JwtUtil을 세팅한다. */
    private void stubValidAccessToken(String token, long userId) {
        when(jwtUtil.isValid(token)).thenReturn(true);
        when(jwtUtil.getType(token)).thenReturn("access");
        when(jwtUtil.getUserId(token)).thenReturn(String.valueOf(userId));
    }

    /** MediaMTX path 조회 응답 스텁. response가 null이면 kickPublisher가 조기 종료한다. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void stubMtxPathLookup(String path, MtxPathResponse response) {
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(mtxRestClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri("v3/paths/get/{name}", path)).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(MtxPathResponse.class)).thenReturn(response);
    }

    /** MediaMTX path 조회가 예외를 던지도록 스텁. kickPublisher의 예외 처리 검증용. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void stubMtxPathLookupFailure(String path, RuntimeException failure) {
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(mtxRestClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri("v3/paths/get/{name}", path)).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(MtxPathResponse.class)).thenThrow(failure);
    }

    /** MediaMTX 세션 강제 종료(kick) 호출 스텁. */
    private void stubMtxKick(String sessionId) {
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        mtxKickUriSpec = mock(RestClient.RequestBodyUriSpec.class);

        when(mtxRestClient.post()).thenReturn(mtxKickUriSpec);
        when(mtxKickUriSpec.uri("v3/webrtcsessions/kick/{id}", sessionId)).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.noContent().build());
    }

    /** Redis SCAN이 주어진 키들을 돌려주도록 스텁. */
    private void stubScan(String... liveKeys) {
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(StreamFixtures.redisCursor(liveKeys));
    }

    /** Redis SCAN 결과가 비어 있도록 스텁. (키가 없어 next()가 호출되지 않는 경우) */
    @SuppressWarnings("unchecked")
    private void stubEmptyScan() {
        Cursor<String> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(false);
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
    }

    /** 라이브 시작/예약 알림 발송 대상 조회 체인 스텁. 팬 수신자만 남도록 구성한다. */
    private void stubNotifyRecipients(long bandId, long broadcasterId, long fanId, boolean started) {
        when(bandMemberPort.getBandSummaryByBandId(bandId))
                .thenReturn(Optional.of(new BandSummaryResponse(bandId, "밴드")));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(
                "livePush:cooldown:" + bandId + ":" + (started ? "started" : "scheduled"),
                "1",
                started ? Duration.ofMinutes(30) : Duration.ofMinutes(5)
        )).thenReturn(true);
        when(followPort.getFollowerUserIdsByBandId(bandId)).thenReturn(List.of(fanId, broadcasterId));
        when(userTermsPort.filterNotificationAgreedUserIds(List.of(fanId, broadcasterId)))
                .thenReturn(List.of(fanId, broadcasterId));
        when(bandMemberPort.getAcceptedMemberUserIds(bandId)).thenReturn(List.of(broadcasterId));
    }

    @Nested
    @DisplayName("canPublish - 송출 티켓 발급")
    class CanPublishTest {

        @Test
        @DisplayName("토큰이 null이면 false를 반환하고 JWT/저장소를 전혀 조회하지 않는다")
        void nullTokenShortCircuits() {
            assertThat(service.canPublish(null, "path-1")).isFalse();

            verifyNoInteractions(jwtUtil);
            verifyNoInteractions(audioStreamRepository);
        }

        @Test
        @DisplayName("유효하지 않은 토큰이면 false를 반환하고 저장소를 조회하지 않는다")
        void invalidTokenReturnsFalse() {
            when(jwtUtil.isValid("bad")).thenReturn(false);

            assertThat(service.canPublish("bad", "path-1")).isFalse();

            verifyNoInteractions(audioStreamRepository);
        }

        @Test
        @DisplayName("access 토큰이 아니면 false를 반환하고 저장소를 조회하지 않는다")
        void refreshTokenReturnsFalse() {
            when(jwtUtil.isValid("refresh-token")).thenReturn(true);
            when(jwtUtil.getType("refresh-token")).thenReturn("refresh");

            assertThat(service.canPublish("refresh-token", "path-1")).isFalse();

            verifyNoInteractions(audioStreamRepository);
        }

        @Test
        @DisplayName("해당 경로의 라이브가 없으면 false를 반환한다")
        void streamNotFoundReturnsFalse() {
            stubValidAccessToken("token", 10L);
            when(audioStreamRepository.findByPath("path-1")).thenReturn(Optional.empty());

            assertThat(service.canPublish("token", "path-1")).isFalse();
        }

        @Test
        @DisplayName("송출자가 아니면 false를 반환한다")
        void otherBroadcasterReturnsFalse() {
            stubValidAccessToken("token", 10L);
            when(audioStreamRepository.findByPath("path-1"))
                    .thenReturn(Optional.of(StreamFixtures.stream(1L, 11L, 100L, StreamStatus.OPEN)));

            assertThat(service.canPublish("token", "path-1")).isFalse();
        }

        @Test
        @DisplayName("OPEN 상태가 아니면 false를 반환한다")
        void notOpenReturnsFalse() {
            stubValidAccessToken("token", 10L);
            when(audioStreamRepository.findByPath("path-1"))
                    .thenReturn(Optional.of(StreamFixtures.stream(1L, 10L, 100L, StreamStatus.SCHEDULED)));

            assertThat(service.canPublish("token", "path-1")).isFalse();
        }

        @Test
        @DisplayName("본인의 OPEN 라이브면 true를 반환한다")
        void ownOpenStreamReturnsTrue() {
            stubValidAccessToken("token", 10L);
            when(audioStreamRepository.findByPath("path-1"))
                    .thenReturn(Optional.of(StreamFixtures.stream(1L, 10L, 100L, StreamStatus.OPEN)));

            assertThat(service.canPublish("token", "path-1")).isTrue();
        }
    }

    @Nested
    @DisplayName("canRead - 청취 티켓 발급")
    class CanReadTest {

        @Test
        @DisplayName("토큰이 null이면 false를 반환하고 JWT/저장소를 전혀 조회하지 않는다")
        void nullTokenShortCircuits() {
            assertThat(service.canRead(null, "path-1")).isFalse();

            verifyNoInteractions(jwtUtil);
            verifyNoInteractions(audioStreamRepository);
        }

        @Test
        @DisplayName("유효하지 않은 토큰이면 false를 반환하고 저장소를 조회하지 않는다")
        void invalidTokenReturnsFalse() {
            when(jwtUtil.isValid("bad")).thenReturn(false);

            assertThat(service.canRead("bad", "path-1")).isFalse();

            verifyNoInteractions(audioStreamRepository);
        }

        @Test
        @DisplayName("access 토큰이 아니면 false를 반환하고 저장소를 조회하지 않는다")
        void refreshTokenReturnsFalse() {
            when(jwtUtil.isValid("refresh-token")).thenReturn(true);
            when(jwtUtil.getType("refresh-token")).thenReturn("refresh");

            assertThat(service.canRead("refresh-token", "path-1")).isFalse();

            verifyNoInteractions(audioStreamRepository);
        }

        @Test
        @DisplayName("로그인 유저면 송출자가 아니어도 OPEN 여부만으로 판단한다")
        void loggedInUserOnlyChecksOpenStatus() {
            stubValidAccessToken("token", 20L);
            when(audioStreamRepository.existsByPathAndStatus("path-1", StreamStatus.OPEN)).thenReturn(true);

            assertThat(service.canRead("token", "path-1")).isTrue();

            verify(audioStreamRepository).existsByPathAndStatus("path-1", StreamStatus.OPEN);
            verify(audioStreamRepository, never()).findByPath(anyString());
        }

        @Test
        @DisplayName("OPEN 라이브가 없으면 false를 반환한다")
        void noOpenStreamReturnsFalse() {
            stubValidAccessToken("token", 20L);
            when(audioStreamRepository.existsByPathAndStatus("path-1", StreamStatus.OPEN)).thenReturn(false);

            assertThat(service.canRead("token", "path-1")).isFalse();
        }
    }

    @Nested
    @DisplayName("createStream - 라이브 생성")
    class CreateStreamTest {

        private StreamCreateRequest request(LocalDateTime scheduledAt) {
            return new StreamCreateRequest("제목", "설명", "https://cdn.test/t.jpg", scheduledAt, null);
        }

        @Test
        @DisplayName("팬 모드면 저장소/포트 조회 전에 FORBIDDEN_REQUEST를 던진다")
        void fanModeIsRejectedBeforeAnyQuery() {
            User fan = StreamFixtures.fanUser(10L);

            assertStreamError(() -> service.createStream(fan, request(null)), StreamErrorCode.FORBIDDEN_REQUEST);

            verifyNoInteractions(audioStreamRepository, streamMemberRepository, bandMemberPort);
        }

        @Test
        @DisplayName("현재 모드가 없는 유저도 FORBIDDEN_REQUEST를 던진다")
        void nullModeIsRejected() {
            User noMode = StreamFixtures.user(10L, null);

            assertStreamError(() -> service.createStream(noMode, request(null)), StreamErrorCode.FORBIDDEN_REQUEST);

            verifyNoInteractions(audioStreamRepository, streamMemberRepository, bandMemberPort);
        }

        @Test
        @DisplayName("예약 라이브가 이미 5개면 TOO_MANY_SCHEDULED_LIVES를 던지고 밴드를 조회하지 않는다")
        void tooManyScheduledLives() {
            User band = StreamFixtures.bandUser(10L);
            when(audioStreamRepository.countByBroadcasterIdAndStatus(10L, StreamStatus.SCHEDULED)).thenReturn(5L);

            assertStreamError(
                    () -> service.createStream(band, request(LocalDateTime.now().plusHours(1))),
                    StreamErrorCode.TOO_MANY_SCHEDULED_LIVES
            );

            verifyNoInteractions(bandMemberPort);
            verify(audioStreamRepository, never()).save(any(AudioStream.class));
        }

        @Test
        @DisplayName("예약 라이브가 4개면 상한에 걸리지 않고 생성된다")
        void scheduledCountBelowLimitPasses() {
            User band = StreamFixtures.bandUser(10L);
            LocalDateTime scheduledAt = LocalDateTime.now().plusHours(1);

            when(audioStreamRepository.countByBroadcasterIdAndStatus(10L, StreamStatus.SCHEDULED)).thenReturn(4L);
            when(bandMemberPort.getBandSummaryByBroadcasterId(10L))
                    .thenReturn(Optional.of(new BandSummaryResponse(100L, "밴드")));
            when(audioStreamRepository.save(any(AudioStream.class))).thenAnswer(i -> i.getArgument(0));
            when(bandMemberPort.getBandSummaryByBandId(100L)).thenReturn(Optional.empty());

            assertThatCode(() -> service.createStream(band, request(scheduledAt))).doesNotThrowAnyException();

            verify(audioStreamRepository).save(any(AudioStream.class));
        }

        @Test
        @DisplayName("scheduledAt이 null이면 예약 개수 쿼리를 아예 실행하지 않는다")
        void immediateLiveSkipsScheduledCountQuery() {
            User band = StreamFixtures.bandUser(10L);
            when(bandMemberPort.getBandSummaryByBroadcasterId(10L))
                    .thenReturn(Optional.of(new BandSummaryResponse(100L, "밴드")));
            when(audioStreamRepository.save(any(AudioStream.class))).thenAnswer(i -> i.getArgument(0));

            service.createStream(band, request(null));

            verify(audioStreamRepository, never())
                    .countByBroadcasterIdAndStatus(anyLong(), any(StreamStatus.class));
        }

        @Test
        @DisplayName("활성 밴드 프로필이 없으면 NO_ACTIVE_BAND_PROFILE을 던진다")
        void noActiveBandProfile() {
            User band = StreamFixtures.bandUser(10L);
            when(bandMemberPort.getBandSummaryByBroadcasterId(10L)).thenReturn(Optional.empty());

            assertStreamError(
                    () -> service.createStream(band, request(null)),
                    StreamErrorCode.NO_ACTIVE_BAND_PROFILE
            );

            verify(audioStreamRepository, never()).save(any(AudioStream.class));
            verifyNoInteractions(streamMemberRepository);
        }

        @Test
        @DisplayName("즉시 라이브 생성 시 AudioStream/StreamMember 필드를 확정하고 알림 훅은 걸지 않는다")
        void savesStreamAndOwnerMemberWithoutNotification() {
            User band = StreamFixtures.bandUser(10L);
            when(bandMemberPort.getBandSummaryByBroadcasterId(10L))
                    .thenReturn(Optional.of(new BandSummaryResponse(100L, "밴드")));
            when(audioStreamRepository.save(any(AudioStream.class))).thenAnswer(i -> i.getArgument(0));

            StreamCreateResponse response = service.createStream(band, request(null));

            ArgumentCaptor<AudioStream> streamCaptor = ArgumentCaptor.forClass(AudioStream.class);
            verify(audioStreamRepository).save(streamCaptor.capture());
            AudioStream saved = streamCaptor.getValue();

            assertThat(saved.getBroadcasterId()).isEqualTo(10L);
            assertThat(saved.getBandId()).isEqualTo(100L);
            assertThat(saved.getStatus()).isEqualTo(StreamStatus.SCHEDULED);
            assertThat(saved.getTitle()).isEqualTo("제목");
            assertThat(saved.getDescription()).isEqualTo("설명");
            assertThat(saved.getThumbnailImageUrl()).isEqualTo("https://cdn.test/t.jpg");
            assertThat(saved.getScheduledAt()).isNull();
            assertThat(saved.getStartedAt()).isNull();
            assertThat(saved.getClosedAt()).isNull();
            assertThat(saved.getPath()).isNotNull();
            assertThatCode(() -> UUID.fromString(saved.getPath())).doesNotThrowAnyException();

            ArgumentCaptor<StreamMember> memberCaptor = ArgumentCaptor.forClass(StreamMember.class);
            verify(streamMemberRepository).save(memberCaptor.capture());
            StreamMember member = memberCaptor.getValue();

            assertThat(member.getUser()).isSameAs(band);
            assertThat(member.getAudioStream()).isSameAs(saved);
            assertThat(member.getStatus()).isEqualTo(StreamMemberStatus.ACCEPTED);

            assertThat(response.path()).isEqualTo(saved.getPath());
            assertThat(response.title()).isEqualTo("제목");

            assertThat(TxSyncSupport.registeredCount()).isZero();
            verifyNoInteractions(notifyPort);
        }

        @Test
        @DisplayName("응답은 저장된 엔티티 기준으로 매핑된다")
        void responseIsMappedFromSavedEntity() {
            User band = StreamFixtures.bandUser(10L);
            AudioStream persisted = StreamFixtures.stream(7L, 10L, 100L, StreamStatus.SCHEDULED);

            when(bandMemberPort.getBandSummaryByBroadcasterId(10L))
                    .thenReturn(Optional.of(new BandSummaryResponse(100L, "밴드")));
            when(audioStreamRepository.save(any(AudioStream.class))).thenReturn(persisted);

            StreamCreateResponse response = service.createStream(band, request(null));

            assertThat(response.audioStreamId()).isEqualTo(7L);
            assertThat(response.path()).isEqualTo("path-7");
            assertThat(response.title()).isEqualTo("title-7");
        }

        @Test
        @DisplayName("예약 라이브 생성 시 커밋 후 알림 훅을 1개 등록한다")
        void scheduledLiveRegistersNotificationHook() {
            User band = StreamFixtures.bandUser(10L);
            LocalDateTime scheduledAt = LocalDateTime.now().plusHours(1);

            when(audioStreamRepository.countByBroadcasterIdAndStatus(10L, StreamStatus.SCHEDULED)).thenReturn(0L);
            when(bandMemberPort.getBandSummaryByBroadcasterId(10L))
                    .thenReturn(Optional.of(new BandSummaryResponse(100L, "밴드")));
            when(audioStreamRepository.save(any(AudioStream.class))).thenAnswer(i -> i.getArgument(0));
            stubNotifyRecipients(100L, 10L, 20L, false);

            service.createStream(band, request(scheduledAt));

            assertThat(TxSyncSupport.registeredCount()).isEqualTo(1);
            verifyNoInteractions(notifyPort);

            TxSyncSupport.triggerAfterCommit();

            verify(notifyPort).notify(eq(List.of(20L)), any(PushMessage.class));
            verifyNoMoreInteractions(notifyPort);
        }

        @Test
        @DisplayName("unique 제약 위반 시 DB_CONSTRAINTS_FAILED로 변환한다")
        void constraintViolationIsTranslated() {
            User band = StreamFixtures.bandUser(10L);
            when(bandMemberPort.getBandSummaryByBroadcasterId(10L))
                    .thenReturn(Optional.of(new BandSummaryResponse(100L, "밴드")));
            when(audioStreamRepository.save(any(AudioStream.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicate path"));

            assertStreamError(
                    () -> service.createStream(band, request(null)),
                    StreamErrorCode.DB_CONSTRAINTS_FAILED
            );

            verifyNoInteractions(streamMemberRepository);
            assertThat(TxSyncSupport.registeredCount()).isZero();
        }
    }

    @Nested
    @DisplayName("closeStream - 라이브 종료")
    class CloseStreamTest {

        @Test
        @DisplayName("라이브가 없으면 AUDIO_STREAM_NOT_FOUND를 던진다")
        void notFound() {
            when(audioStreamRepository.findById(1L)).thenReturn(Optional.empty());

            assertStreamError(() -> service.closeStream(10L, 1L), StreamErrorCode.AUDIO_STREAM_NOT_FOUND);

            assertThat(TxSyncSupport.registeredCount()).isZero();
        }

        @Test
        @DisplayName("송출자가 아니면 FORBIDDEN_REQUEST를 던지고 밴드 검증도 하지 않는다")
        void notBroadcaster() {
            when(audioStreamRepository.findById(1L))
                    .thenReturn(Optional.of(StreamFixtures.stream(1L, 11L, 100L, StreamStatus.OPEN)));

            assertStreamError(() -> service.closeStream(10L, 1L), StreamErrorCode.FORBIDDEN_REQUEST);

            verifyNoInteractions(bandMemberPort);
            verifyNoInteractions(redisTemplate);
            assertThat(TxSyncSupport.registeredCount()).isZero();
        }

        @Test
        @DisplayName("활성 밴드가 라이브의 밴드와 다르면 FORBIDDEN_REQUEST를 던진다")
        void bandMismatch() {
            when(audioStreamRepository.findById(1L))
                    .thenReturn(Optional.of(StreamFixtures.stream(1L, 10L, 100L, StreamStatus.OPEN)));
            when(bandMemberPort.isActiveRegularMemberOfBand(100L, 10L)).thenReturn(false);

            assertStreamError(() -> service.closeStream(10L, 1L), StreamErrorCode.FORBIDDEN_REQUEST);

            verifyNoInteractions(redisTemplate);
            assertThat(TxSyncSupport.registeredCount()).isZero();
        }

        @Test
        @DisplayName("종료 시 CLOSED 전환·시청자 수 스냅샷·Redis 키 삭제를 수행하고, 강제 해제/채팅 종료는 커밋 이후에만 실행한다")
        void closesAndDefersSideEffectsUntilCommit() {
            AudioStream stream = StreamFixtures.stream(1L, 10L, 100L, StreamStatus.OPEN);
            when(audioStreamRepository.findById(1L)).thenReturn(Optional.of(stream));
            when(bandMemberPort.isActiveRegularMemberOfBand(100L, 10L)).thenReturn(true);
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.zCard("viewer:1")).thenReturn(7L);
            stubMtxPathLookup("path-1", null);

            service.closeStream(10L, 1L);

            assertThat(stream.getStatus()).isEqualTo(StreamStatus.CLOSED);
            assertThat(stream.getClosedAt()).isNotNull();
            assertThat(stream.getClosedViewerCount()).isEqualTo(7);
            verify(redisTemplate).delete("live:path-1");

            // 커밋 전에는 외부 부수효과가 일어나면 안 된다
            verify(mtxRestClient, never()).get();
            verifyNoInteractions(liveChatRoomCloser);
            assertThat(TxSyncSupport.registeredCount()).isEqualTo(1);

            TxSyncSupport.triggerAfterCommit();

            verify(mtxRestClient).get();
            verify(liveChatRoomCloser).close(1L);
        }

        @Test
        @DisplayName("시청자 ZSet이 비어 zCard가 null이면 스냅샷은 0이다")
        void nullZCardSnapshotsZero() {
            AudioStream stream = StreamFixtures.stream(1L, 10L, 100L, StreamStatus.OPEN);
            when(audioStreamRepository.findById(1L)).thenReturn(Optional.of(stream));
            when(bandMemberPort.isActiveRegularMemberOfBand(100L, 10L)).thenReturn(true);
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.zCard("viewer:1")).thenReturn(null);

            service.closeStream(10L, 1L);

            assertThat(stream.getClosedViewerCount()).isZero();
        }

        @Test
        @DisplayName("커밋 후 WebRTC 송출 세션이 남아 있으면 해당 세션을 kick 한다")
        void kicksWebrtcPublisherAfterCommit() {
            AudioStream stream = StreamFixtures.stream(1L, 10L, 100L, StreamStatus.OPEN);
            when(audioStreamRepository.findById(1L)).thenReturn(Optional.of(stream));
            when(bandMemberPort.isActiveRegularMemberOfBand(100L, 10L)).thenReturn(true);
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.zCard("viewer:1")).thenReturn(0L);
            stubMtxPathLookup("path-1", new MtxPathResponse(new MtxPathResponse.Source("webRTCSession", "session-1")));
            stubMtxKick("session-1");

            service.closeStream(10L, 1L);
            TxSyncSupport.triggerAfterCommit();

            verify(mtxKickUriSpec).uri("v3/webrtcsessions/kick/{id}", "session-1");
            verify(liveChatRoomCloser).close(1L);
        }

        @Test
        @DisplayName("WebRTC 송출 세션이 아니면 kick 요청을 보내지 않는다")
        void doesNotKickWhenSourceIsNotWebrtc() {
            AudioStream stream = StreamFixtures.stream(1L, 10L, 100L, StreamStatus.OPEN);
            when(audioStreamRepository.findById(1L)).thenReturn(Optional.of(stream));
            when(bandMemberPort.isActiveRegularMemberOfBand(100L, 10L)).thenReturn(true);
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.zCard("viewer:1")).thenReturn(0L);
            stubMtxPathLookup("path-1", new MtxPathResponse(new MtxPathResponse.Source("rtmpConn", "conn-1")));

            service.closeStream(10L, 1L);
            TxSyncSupport.triggerAfterCommit();

            verify(mtxRestClient, never()).post();
            verify(liveChatRoomCloser).close(1L);
        }

        @Test
        @DisplayName("path 조회 응답에 source가 없으면 kick 요청을 보내지 않는다")
        void doesNotKickWhenSourceIsNull() {
            AudioStream stream = StreamFixtures.stream(1L, 10L, 100L, StreamStatus.OPEN);
            when(audioStreamRepository.findById(1L)).thenReturn(Optional.of(stream));
            when(bandMemberPort.isActiveRegularMemberOfBand(100L, 10L)).thenReturn(true);
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.zCard("viewer:1")).thenReturn(0L);
            stubMtxPathLookup("path-1", new MtxPathResponse(null));

            service.closeStream(10L, 1L);
            TxSyncSupport.triggerAfterCommit();

            verify(mtxRestClient, never()).post();
            verify(liveChatRoomCloser).close(1L);
        }

        @Test
        @DisplayName("MediaMTX에 path가 이미 없으면(404) 조용히 넘어가고 채팅 종료는 그대로 진행한다")
        void swallowsNotFoundFromMtxAndStillClosesChat() {
            AudioStream stream = StreamFixtures.stream(1L, 10L, 100L, StreamStatus.OPEN);
            when(audioStreamRepository.findById(1L)).thenReturn(Optional.of(stream));
            when(bandMemberPort.isActiveRegularMemberOfBand(100L, 10L)).thenReturn(true);
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.zCard("viewer:1")).thenReturn(0L);
            stubMtxPathLookupFailure("path-1", (HttpClientErrorException) HttpClientErrorException.create(
                    HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, new byte[0], null));

            service.closeStream(10L, 1L);

            assertThatCode(TxSyncSupport::triggerAfterCommit).doesNotThrowAnyException();

            verify(mtxRestClient, never()).post();
            verify(liveChatRoomCloser).close(1L);
        }

        @Test
        @DisplayName("MediaMTX 호출이 404 외의 이유로 실패해도 커밋 훅이 죽지 않고 채팅 종료는 진행한다")
        void swallowsRestClientErrorAndStillClosesChat() {
            AudioStream stream = StreamFixtures.stream(1L, 10L, 100L, StreamStatus.OPEN);
            when(audioStreamRepository.findById(1L)).thenReturn(Optional.of(stream));
            when(bandMemberPort.isActiveRegularMemberOfBand(100L, 10L)).thenReturn(true);
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.zCard("viewer:1")).thenReturn(0L);
            stubMtxPathLookupFailure("path-1", new RestClientException("MediaMTX 연결 실패"));

            service.closeStream(10L, 1L);

            assertThatCode(TxSyncSupport::triggerAfterCommit).doesNotThrowAnyException();

            verify(mtxRestClient, never()).post();
            verify(liveChatRoomCloser).close(1L);
        }
    }

    @Nested
    @DisplayName("enterRoom - 송출자 진입")
    class EnterRoomBroadcasterTest {

        @Test
        @DisplayName("라이브가 없으면 AUDIO_STREAM_NOT_FOUND를 던진다")
        void notFound() {
            when(audioStreamRepository.findById(1L)).thenReturn(Optional.empty());

            assertStreamError(() -> service.enterRoom(10L, 1L), StreamErrorCode.AUDIO_STREAM_NOT_FOUND);
        }

        @Test
        @DisplayName("CLOSED 라이브에 진입하면 AUDIO_STREAM_NOT_LIVE를 던진다")
        void closedStreamRejected() {
            when(audioStreamRepository.findById(1L))
                    .thenReturn(Optional.of(StreamFixtures.stream(1L, 10L, 100L, StreamStatus.CLOSED)));

            assertStreamError(() -> service.enterRoom(10L, 1L), StreamErrorCode.AUDIO_STREAM_NOT_LIVE);

            verifyNoInteractions(bandMemberPort, redisTemplate);
        }

        @Test
        @DisplayName("CANCELED 라이브에 진입하면 AUDIO_STREAM_NOT_LIVE를 던진다")
        void canceledStreamRejected() {
            when(audioStreamRepository.findById(1L))
                    .thenReturn(Optional.of(StreamFixtures.stream(1L, 10L, 100L, StreamStatus.CANCELED)));

            assertStreamError(() -> service.enterRoom(10L, 1L), StreamErrorCode.AUDIO_STREAM_NOT_LIVE);

            verifyNoInteractions(bandMemberPort, redisTemplate);
        }

        @Test
        @DisplayName("SCHEDULED 라이브 시작 시 활성 밴드가 다르면 FORBIDDEN_REQUEST를 던진다")
        void scheduledStartRequiresActiveBand() {
            when(audioStreamRepository.findById(1L))
                    .thenReturn(Optional.of(StreamFixtures.scheduledStream(1L, 10L, 100L, LocalDateTime.now().plusHours(1))));
            when(bandMemberPort.isActiveRegularMemberOfBand(100L, 10L)).thenReturn(false);

            assertStreamError(() -> service.enterRoom(10L, 1L), StreamErrorCode.FORBIDDEN_REQUEST);

            verify(audioStreamRepository, never()).existsByBroadcasterIdAndStatus(anyLong(), any(StreamStatus.class));
        }

        @Test
        @DisplayName("이미 진행 중인 OPEN 라이브가 있으면 ALREADY_LIVE를 던진다")
        void alreadyLive() {
            when(audioStreamRepository.findById(1L))
                    .thenReturn(Optional.of(StreamFixtures.scheduledStream(1L, 10L, 100L, LocalDateTime.now().plusHours(1))));
            when(bandMemberPort.isActiveRegularMemberOfBand(100L, 10L)).thenReturn(true);
            when(audioStreamRepository.existsByBroadcasterIdAndStatus(10L, StreamStatus.OPEN)).thenReturn(true);

            assertStreamError(() -> service.enterRoom(10L, 1L), StreamErrorCode.ALREADY_LIVE);

            verify(audioStreamRepository, never()).markStartedIfScheduled(anyLong(), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("조건부 UPDATE가 0건이면 AUDIO_STREAM_NOT_SCHEDULED를 던진다")
        void markStartedNoRowUpdated() {
            when(audioStreamRepository.findById(1L))
                    .thenReturn(Optional.of(StreamFixtures.scheduledStream(1L, 10L, 100L, LocalDateTime.now().plusHours(1))));
            when(bandMemberPort.isActiveRegularMemberOfBand(100L, 10L)).thenReturn(true);
            when(audioStreamRepository.existsByBroadcasterIdAndStatus(10L, StreamStatus.OPEN)).thenReturn(false);
            when(audioStreamRepository.markStartedIfScheduled(eq(1L), any(LocalDateTime.class))).thenReturn(0);

            assertStreamError(() -> service.enterRoom(10L, 1L), StreamErrorCode.AUDIO_STREAM_NOT_SCHEDULED);

            verify(audioStreamRepository, times(1)).findById(1L);
            assertThat(TxSyncSupport.registeredCount()).isZero();
        }

        @Test
        @DisplayName("시작 전환 후 재조회에서 라이브가 사라졌으면 AUDIO_STREAM_NOT_FOUND를 던지고 알림 훅을 등록하지 않는다")
        void refetchAfterMarkStartedMissing() {
            AudioStream scheduled = StreamFixtures.scheduledStream(1L, 10L, 100L, LocalDateTime.now().plusHours(1));
            when(audioStreamRepository.findById(1L))
                    .thenReturn(Optional.of(scheduled), Optional.empty());
            when(bandMemberPort.isActiveRegularMemberOfBand(100L, 10L)).thenReturn(true);
            when(audioStreamRepository.existsByBroadcasterIdAndStatus(10L, StreamStatus.OPEN)).thenReturn(false);
            when(audioStreamRepository.markStartedIfScheduled(eq(1L), any(LocalDateTime.class))).thenReturn(1);

            assertStreamError(() -> service.enterRoom(10L, 1L), StreamErrorCode.AUDIO_STREAM_NOT_FOUND);

            verify(audioStreamRepository, times(2)).findById(1L);
            assertThat(TxSyncSupport.registeredCount()).isZero();
        }

        @Test
        @DisplayName("SCHEDULED 라이브를 시작하면 재조회 후 시작 알림 훅을 등록하고 WHIP 재생 정보를 준다")
        void startsScheduledLiveAndRegistersStartedNotification() {
            AudioStream scheduled = StreamFixtures.scheduledStream(1L, 10L, 100L, LocalDateTime.now().plusHours(1));
            AudioStream opened = StreamFixtures.stream(1L, 10L, 100L, StreamStatus.OPEN);

            when(audioStreamRepository.findById(1L))
                    .thenReturn(Optional.of(scheduled), Optional.of(opened));
            when(bandMemberPort.isActiveRegularMemberOfBand(100L, 10L)).thenReturn(true);
            when(audioStreamRepository.existsByBroadcasterIdAndStatus(10L, StreamStatus.OPEN)).thenReturn(false);
            when(audioStreamRepository.markStartedIfScheduled(eq(1L), any(LocalDateTime.class))).thenReturn(1);
            stubNotifyRecipients(100L, 10L, 20L, true);
            when(redisTemplate.hasKey("live:path-1")).thenReturn(true);
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.zCard("viewer:1")).thenReturn(3L);
            when(bandMemberPort.getBandNameWithBandProfileByBroadcasterId(Set.of(10L)))
                    .thenReturn(List.of(StreamFixtures.bandInfo(10L, "밴드", "https://cdn.test/band.jpg")));

            StreamRoomResponse response = service.enterRoom(10L, 1L);

            // 1차 조회 + 벌크 UPDATE 이후 재조회
            verify(audioStreamRepository, times(2)).findById(1L);
            assertThat(response.liveId()).isEqualTo(1L);
            assertThat(response.isLive()).isTrue();
            assertThat(response.viewCount()).isEqualTo(3);
            assertThat(response.bandName()).isEqualTo("밴드");
            assertThat(response.bandProfileImageUrl()).isEqualTo("https://cdn.test/band.jpg");
            assertThat(response.playback()).isEqualTo(new StreamRoomResponse.Playback(
                    "BROADCASTER", "WHIP", WEBRTC_URL + "/path-1/whip"
            ));

            // 송출자는 시청자 ZSet에 등록되지 않는다
            verify(zSetOperations, never()).add(anyString(), anyString(), org.mockito.ArgumentMatchers.anyDouble());
            verify(viewerSsePresence, never()).broadcastCount(anyLong());

            assertThat(TxSyncSupport.registeredCount()).isEqualTo(1);
            verifyNoInteractions(notifyPort);

            TxSyncSupport.triggerAfterCommit();

            verify(notifyPort).notify(eq(List.of(20L)), any(PushMessage.class));
        }

        @Test
        @DisplayName("이미 OPEN인 라이브 재입장은 밴드 검증과 시작 처리를 건너뛴다")
        void reentryToOpenLiveSkipsStartFlow() {
            when(audioStreamRepository.findById(1L))
                    .thenReturn(Optional.of(StreamFixtures.stream(1L, 10L, 100L, StreamStatus.OPEN)));
            when(redisTemplate.hasKey("live:path-1")).thenReturn(true);
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.zCard("viewer:1")).thenReturn(1L);
            when(bandMemberPort.getBandNameWithBandProfileByBroadcasterId(Set.of(10L))).thenReturn(List.of());

            StreamRoomResponse response = service.enterRoom(10L, 1L);

            verify(audioStreamRepository, times(1)).findById(1L);
            verify(bandMemberPort, never()).isActiveRegularMemberOfBand(anyLong(), anyLong());
            verify(audioStreamRepository, never()).existsByBroadcasterIdAndStatus(anyLong(), any(StreamStatus.class));
            verify(audioStreamRepository, never()).markStartedIfScheduled(anyLong(), any(LocalDateTime.class));
            assertThat(TxSyncSupport.registeredCount()).isZero();
            assertThat(response.playback().role()).isEqualTo("BROADCASTER");
        }
    }

    @Nested
    @DisplayName("enterRoom - 청취자 진입")
    class EnterRoomListenerTest {

        @Test
        @DisplayName("OPEN이 아닌 라이브에 진입하면 AUDIO_STREAM_NOT_LIVE를 던진다")
        void notOpenRejected() {
            when(audioStreamRepository.findById(1L))
                    .thenReturn(Optional.of(StreamFixtures.scheduledStream(1L, 10L, 100L, LocalDateTime.now().plusHours(1))));

            assertStreamError(() -> service.enterRoom(20L, 1L), StreamErrorCode.AUDIO_STREAM_NOT_LIVE);

            verifyNoInteractions(redisTemplate, viewerSsePresence);
        }

        @Test
        @DisplayName("진입 시 시청자 ZSet에 현재 epoch초 점수로 등록하고 카운트를 브로드캐스트한다")
        void registersViewerAndBroadcasts() {
            when(audioStreamRepository.findById(1L))
                    .thenReturn(Optional.of(StreamFixtures.stream(1L, 10L, 100L, StreamStatus.OPEN)));
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.zCard("viewer:1")).thenReturn(5L);
            when(redisTemplate.hasKey("live:path-1")).thenReturn(true);
            when(bandMemberPort.getBandNameWithBandProfileByBroadcasterId(Set.of(10L)))
                    .thenReturn(List.of(StreamFixtures.bandInfo(10L, "밴드", "https://cdn.test/band.jpg")));

            StreamRoomResponse response = service.enterRoom(20L, 1L);

            ArgumentCaptor<Double> score = ArgumentCaptor.forClass(Double.class);
            verify(zSetOperations).add(eq("viewer:1"), eq("20"), score.capture());
            assertThat(score.getValue()).isCloseTo((double) Instant.now().getEpochSecond(), within(5.0));

            verify(viewerSsePresence).broadcastCount(1L);

            assertThat(response.isLive()).isTrue();
            assertThat(response.viewCount()).isEqualTo(5);
            assertThat(response.playback()).isEqualTo(new StreamRoomResponse.Playback(
                    "LISTENER", "HLS", HLS_URL + "/path-1/index.m3u8"
            ));
        }

        @Test
        @DisplayName("Redis 라이브 키가 없으면 isLive=false, playback은 null이다")
        void noLiveKeyMeansNullPlayback() {
            when(audioStreamRepository.findById(1L))
                    .thenReturn(Optional.of(StreamFixtures.stream(1L, 10L, 100L, StreamStatus.OPEN)));
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.zCard("viewer:1")).thenReturn(0L);
            when(redisTemplate.hasKey("live:path-1")).thenReturn(false);
            when(bandMemberPort.getBandNameWithBandProfileByBroadcasterId(Set.of(10L)))
                    .thenReturn(List.of(StreamFixtures.bandInfo(10L, "밴드", "https://cdn.test/band.jpg")));

            StreamRoomResponse response = service.enterRoom(20L, 1L);

            assertThat(response.isLive()).isFalse();
            assertThat(response.playback()).isNull();
        }

        @Test
        @DisplayName("zCard가 null이면 viewCount는 0이고 밴드 정보가 없으면 빈 문자열로 채운다")
        void nullViewCountAndMissingBandInfo() {
            when(audioStreamRepository.findById(1L))
                    .thenReturn(Optional.of(StreamFixtures.stream(1L, 10L, 100L, StreamStatus.OPEN)));
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.zCard("viewer:1")).thenReturn(null);
            when(redisTemplate.hasKey("live:path-1")).thenReturn(null);
            when(bandMemberPort.getBandNameWithBandProfileByBroadcasterId(Set.of(10L))).thenReturn(List.of());

            StreamRoomResponse response = service.enterRoom(20L, 1L);

            assertThat(response.viewCount()).isZero();
            assertThat(response.isLive()).isFalse();
            assertThat(response.bandName()).isEmpty();
            assertThat(response.bandProfileImageUrl()).isEmpty();
            assertThat(response.title()).isEqualTo("title-1");
            assertThat(response.description()).isEqualTo("description-1");
            assertThat(response.playback()).isNull();
        }
    }

    @Nested
    @DisplayName("leaveRoom - 청취자 퇴장")
    class LeaveRoomTest {

        @Test
        @DisplayName("ZSet에서 제거한 뒤에 시청자 수를 브로드캐스트한다")
        void removesThenBroadcastsInOrder() {
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

            service.leaveRoom(20L, 1L);

            InOrder inOrder = inOrder(zSetOperations, viewerSsePresence);
            inOrder.verify(zSetOperations).remove("viewer:1", "20");
            inOrder.verify(viewerSsePresence).broadcastCount(1L);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("subscribeViewerCount - SSE 구독 위임")
    class SubscribeViewerCountTest {

        @Test
        @DisplayName("watchOnly=false 인자를 그대로 ViewerSsePresence에 위임한다")
        void delegatesWithWatchOnlyFalse() {
            SseEmitter emitter = new SseEmitter();
            when(viewerSsePresence.subscribe(20L, 1L, false)).thenReturn(emitter);

            assertThat(service.subscribeViewerCount(20L, 1L, false)).isSameAs(emitter);

            verify(viewerSsePresence).subscribe(20L, 1L, false);
            verifyNoMoreInteractions(viewerSsePresence);
            verifyNoInteractions(audioStreamRepository, redisTemplate);
        }

        @Test
        @DisplayName("watchOnly=true 인자를 그대로 ViewerSsePresence에 위임한다")
        void delegatesWithWatchOnlyTrue() {
            SseEmitter emitter = new SseEmitter();
            when(viewerSsePresence.subscribe(20L, 1L, true)).thenReturn(emitter);

            assertThat(service.subscribeViewerCount(20L, 1L, true)).isSameAs(emitter);

            verify(viewerSsePresence).subscribe(20L, 1L, true);
            verifyNoMoreInteractions(viewerSsePresence);
        }
    }

    @Nested
    @DisplayName("getStreamSummary - 종료 요약")
    class GetStreamSummaryTest {

        @Test
        @DisplayName("라이브가 없으면 AUDIO_STREAM_NOT_FOUND를 던진다")
        void notFound() {
            when(audioStreamRepository.findById(1L)).thenReturn(Optional.empty());

            assertStreamError(() -> service.getStreamSummary(10L, 1L), StreamErrorCode.AUDIO_STREAM_NOT_FOUND);
        }

        @Test
        @DisplayName("송출자가 아니면 FORBIDDEN_REQUEST를 던지고 밴드 검증을 하지 않는다")
        void notBroadcaster() {
            when(audioStreamRepository.findById(1L))
                    .thenReturn(Optional.of(StreamFixtures.closedStream(
                            1L, 11L, 100L, LocalDateTime.now().minusHours(1), LocalDateTime.now(), 3)));

            assertStreamError(() -> service.getStreamSummary(10L, 1L), StreamErrorCode.FORBIDDEN_REQUEST);

            verifyNoInteractions(bandMemberPort);
        }

        @Test
        @DisplayName("활성 밴드가 라이브의 밴드와 다르면 FORBIDDEN_REQUEST를 던진다")
        void bandMismatch() {
            when(audioStreamRepository.findById(1L))
                    .thenReturn(Optional.of(StreamFixtures.closedStream(
                            1L, 10L, 100L, LocalDateTime.now().minusHours(1), LocalDateTime.now(), 3)));
            when(bandMemberPort.isActiveRegularMemberOfBand(100L, 10L)).thenReturn(false);

            assertStreamError(() -> service.getStreamSummary(10L, 1L), StreamErrorCode.FORBIDDEN_REQUEST);
        }

        @Test
        @DisplayName("아직 종료되지 않은 라이브면 STREAM_NOT_CLOSED를 던진다")
        void notClosed() {
            when(audioStreamRepository.findById(1L))
                    .thenReturn(Optional.of(StreamFixtures.stream(1L, 10L, 100L, StreamStatus.OPEN)));
            when(bandMemberPort.isActiveRegularMemberOfBand(100L, 10L)).thenReturn(true);

            assertStreamError(() -> service.getStreamSummary(10L, 1L), StreamErrorCode.STREAM_NOT_CLOSED);
        }

        @Test
        @DisplayName("startedAt~closedAt 차이를 초 단위 방송 시간으로 계산한다")
        void computesDurationSeconds() {
            LocalDateTime startedAt = LocalDateTime.of(2026, 7, 11, 21, 0, 0);
            when(audioStreamRepository.findById(1L))
                    .thenReturn(Optional.of(StreamFixtures.closedStream(
                            1L, 10L, 100L, startedAt, startedAt.plusSeconds(3661), 42)));
            when(bandMemberPort.isActiveRegularMemberOfBand(100L, 10L)).thenReturn(true);

            StreamSummaryResponse response = service.getStreamSummary(10L, 1L);

            assertThat(response.title()).isEqualTo("title-1");
            assertThat(response.durationSec()).isEqualTo(3661);
            assertThat(response.closedViewerCount()).isEqualTo(42);
        }

        @Test
        @DisplayName("startedAt이 null이면 방송 시간은 0이다")
        void nullStartedAtMeansZeroDuration() {
            when(audioStreamRepository.findById(1L))
                    .thenReturn(Optional.of(StreamFixtures.closedStream(
                            1L, 10L, 100L, null, LocalDateTime.now(), 5)));
            when(bandMemberPort.isActiveRegularMemberOfBand(100L, 10L)).thenReturn(true);

            assertThat(service.getStreamSummary(10L, 1L).durationSec()).isZero();
        }

        @Test
        @DisplayName("closedAt이 null이면 방송 시간은 0이다")
        void nullClosedAtMeansZeroDuration() {
            when(audioStreamRepository.findById(1L))
                    .thenReturn(Optional.of(StreamFixtures.closedStream(
                            1L, 10L, 100L, LocalDateTime.now().minusHours(1), null, 5)));
            when(bandMemberPort.isActiveRegularMemberOfBand(100L, 10L)).thenReturn(true);

            assertThat(service.getStreamSummary(10L, 1L).durationSec()).isZero();
        }

        @Test
        @DisplayName("종료 시청자 수 스냅샷이 null이면 0으로 응답한다")
        void nullClosedViewerCountMeansZero() {
            LocalDateTime startedAt = LocalDateTime.of(2026, 7, 11, 21, 0, 0);
            when(audioStreamRepository.findById(1L))
                    .thenReturn(Optional.of(StreamFixtures.closedStream(
                            1L, 10L, 100L, startedAt, startedAt.plusMinutes(1), null)));
            when(bandMemberPort.isActiveRegularMemberOfBand(100L, 10L)).thenReturn(true);

            StreamSummaryResponse response = service.getStreamSummary(10L, 1L);

            assertThat(response.closedViewerCount()).isZero();
            assertThat(response.durationSec()).isEqualTo(60);
        }
    }

    @Nested
    @DisplayName("syncLiveState - MediaMTX 상태 동기화")
    class SyncLiveStateTest {

        private static final Duration LIVE_TTL = Duration.ofSeconds(15);

        @Test
        @DisplayName("새로 켜진 방송은 15초 TTL로 키를 새로 쓴다 (TTL 연장 아님)")
        void newReadyPathIsSet() {
            stubEmptyScan();
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            service.syncLiveState(Set.of("path-1"));

            verify(valueOperations).set("live:path-1", "1", LIVE_TTL);
            verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
            verify(redisTemplate, never()).delete(anyString());
            verifyNoInteractions(audioStreamRepository);
        }

        @Test
        @DisplayName("이미 등록된 방송은 TTL만 연장하고 키를 다시 쓰지 않는다")
        void knownPathOnlyExtendsTtl() {
            stubScan("live:path-1");

            service.syncLiveState(Set.of("path-1"));

            verify(redisTemplate).expire("live:path-1", LIVE_TTL);
            verify(redisTemplate, never()).opsForValue();
            verify(redisTemplate, never()).delete(anyString());
            verifyNoInteractions(audioStreamRepository);
        }

        @Test
        @DisplayName("Redis에만 남은 경로는 키를 삭제하고 시청자 수 스냅샷과 함께 라이브를 종료한다")
        void orphanPathIsClosed() {
            AudioStream stream = StreamFixtures.stream(1L, 10L, 100L, StreamStatus.OPEN);
            stubScan("live:path-1");
            when(audioStreamRepository.findByPath("path-1")).thenReturn(Optional.of(stream));
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.zCard("viewer:1")).thenReturn(9L);

            service.syncLiveState(Set.of());

            verify(redisTemplate).delete("live:path-1");
            assertThat(stream.getStatus()).isEqualTo(StreamStatus.CLOSED);
            assertThat(stream.getClosedAt()).isNotNull();
            assertThat(stream.getClosedViewerCount()).isEqualTo(9);
            verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
            verify(redisTemplate, never()).opsForValue();
        }

        @Test
        @DisplayName("Redis에만 남은 경로에 해당하는 라이브 레코드가 없어도 키 삭제만 하고 예외 없이 끝낸다")
        void orphanPathWithoutRowJustDeletes() {
            stubScan("live:path-1");
            when(audioStreamRepository.findByPath("path-1")).thenReturn(Optional.empty());

            assertThatCode(() -> service.syncLiveState(Set.of())).doesNotThrowAnyException();

            verify(redisTemplate).delete("live:path-1");
            verify(redisTemplate, never()).opsForZSet();
        }

        @Test
        @DisplayName("ready 경로와 Redis가 모두 비어 있으면 SCAN 외에는 아무것도 하지 않는다")
        void nothingToDo() {
            stubEmptyScan();

            service.syncLiveState(Set.of());

            verify(redisTemplate).scan(any(ScanOptions.class));
            verifyNoMoreInteractions(redisTemplate);
            verifyNoInteractions(audioStreamRepository, viewerSsePresence, notifyPort);
        }

        @Test
        @DisplayName("신규 등록과 고아 키 정리가 한 번의 호출에서 함께 처리된다")
        void mixedSetAndCleanup() {
            AudioStream stale = StreamFixtures.stream(2L, 10L, 100L, StreamStatus.OPEN);
            stubScan("live:path-2");
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(audioStreamRepository.findByPath("path-2")).thenReturn(Optional.of(stale));
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.zCard("viewer:2")).thenReturn(null);

            service.syncLiveState(Set.of("path-1"));

            verify(valueOperations).set("live:path-1", "1", LIVE_TTL);
            verify(redisTemplate).delete("live:path-2");
            assertThat(stale.getStatus()).isEqualTo(StreamStatus.CLOSED);
            assertThat(stale.getClosedViewerCount()).isZero();
        }
    }
}
