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
import org.mockito.Captor;
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
import java.util.Collection;
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
    private static final String MIXER_TOKEN = "mixer-secret";

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

    @Captor private ArgumentCaptor<Collection<Long>> coHostIdsCaptor;

    private RestClient.RequestBodyUriSpec mtxKickUriSpec;
    @SuppressWarnings("rawtypes")
    private RestClient.RequestHeadersUriSpec mtxGetUriSpec;

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
                WEBRTC_URL,
                MIXER_TOKEN
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

    /** path 조회 uriSpec을 공유해 한 테스트에서 여러 path(메인 + 멤버)를 함께 스텁할 수 있게 한다. */
    @SuppressWarnings("rawtypes")
    private RestClient.RequestHeadersUriSpec mtxGetUriSpec() {
        if (mtxGetUriSpec == null) {
            mtxGetUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
            when(mtxRestClient.get()).thenReturn(mtxGetUriSpec);
        }
        return mtxGetUriSpec;
    }

    /** MediaMTX path 조회 응답 스텁. response가 null이면 kickPublisher가 조기 종료한다. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void stubMtxPathLookup(String path, MtxPathResponse response) {
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(mtxGetUriSpec().uri("v3/paths/get/{name}", path)).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(MtxPathResponse.class)).thenReturn(response);
    }

    /** MediaMTX path 조회가 예외를 던지도록 스텁. kickPublisher의 예외 처리 검증용. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void stubMtxPathLookupFailure(String path, RuntimeException failure) {
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(mtxGetUriSpec().uri("v3/paths/get/{name}", path)).thenReturn(headersSpec);
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

        /** OPEN 라이브의 ACCEPTED 진행자 멤버(개인 송출 path 발급 완료 상태)를 만든다. */
        private StreamMember memberWithPath(Long memberId, Long userId, StreamStatus streamStatus) {
            AudioStream stream = StreamFixtures.stream(1L, 10L, 100L, streamStatus);
            StreamMember member = StreamFixtures.member(
                    memberId, StreamFixtures.bandUser(userId), stream, StreamMemberStatus.ACCEPTED);
            member.assignPath("path-1-m" + memberId);
            return member;
        }

        @Test
        @DisplayName("해당 멤버 path가 발급된 적 없으면 false를 반환한다")
        void memberPathNotFoundReturnsFalse() {
            stubValidAccessToken("token", 10L);
            when(streamMemberRepository.findWithUserAndStreamByPath("path-1-m901")).thenReturn(Optional.empty());

            assertThat(service.canPublish("token", "path-1-m901")).isFalse();
        }

        @Test
        @DisplayName("본인 소유 path가 아니면 false를 반환한다")
        void otherMemberPathReturnsFalse() {
            stubValidAccessToken("token", 10L);
            when(streamMemberRepository.findWithUserAndStreamByPath("path-1-m902"))
                    .thenReturn(Optional.of(memberWithPath(902L, 11L, StreamStatus.OPEN)));

            assertThat(service.canPublish("token", "path-1-m902")).isFalse();
        }

        @Test
        @DisplayName("공동 진행이 확정(ACCEPTED)되지 않은 멤버면 false를 반환한다")
        void notAcceptedMemberReturnsFalse() {
            stubValidAccessToken("token", 10L);
            StreamMember invited = StreamFixtures.member(
                    901L, StreamFixtures.bandUser(10L),
                    StreamFixtures.stream(1L, 10L, 100L, StreamStatus.OPEN),
                    StreamMemberStatus.INVITED);
            when(streamMemberRepository.findWithUserAndStreamByPath("path-1-m901"))
                    .thenReturn(Optional.of(invited));

            assertThat(service.canPublish("token", "path-1-m901")).isFalse();
        }

        @Test
        @DisplayName("소속 라이브가 OPEN 상태가 아니면 false를 반환한다")
        void notOpenReturnsFalse() {
            stubValidAccessToken("token", 10L);
            when(streamMemberRepository.findWithUserAndStreamByPath("path-1-m901"))
                    .thenReturn(Optional.of(memberWithPath(901L, 10L, StreamStatus.SCHEDULED)));

            assertThat(service.canPublish("token", "path-1-m901")).isFalse();
        }

        @Test
        @DisplayName("본인 멤버 path이고 소속 라이브가 OPEN이면 true를 반환한다")
        void ownMemberPathOfOpenStreamReturnsTrue() {
            stubValidAccessToken("token", 10L);
            when(streamMemberRepository.findWithUserAndStreamByPath("path-1-m901"))
                    .thenReturn(Optional.of(memberWithPath(901L, 10L, StreamStatus.OPEN)));

            assertThat(service.canPublish("token", "path-1-m901")).isTrue();
        }

        @Test
        @DisplayName("믹서 토큰이면 JWT 검증 없이 메인 path의 OPEN 여부만으로 판단한다")
        void mixerTokenPublishesMainPath() {
            when(audioStreamRepository.existsByPathAndStatus("path-1", StreamStatus.OPEN)).thenReturn(true);

            assertThat(service.canPublish(MIXER_TOKEN, "path-1")).isTrue();

            verifyNoInteractions(jwtUtil);
            verifyNoInteractions(streamMemberRepository);
        }

        @Test
        @DisplayName("믹서 토큰이라도 OPEN 라이브가 아닌 메인 path면 false를 반환한다")
        void mixerTokenRejectedWhenNotOpen() {
            when(audioStreamRepository.existsByPathAndStatus("path-9", StreamStatus.OPEN)).thenReturn(false);

            assertThat(service.canPublish(MIXER_TOKEN, "path-9")).isFalse();
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

        @Test
        @DisplayName("믹서 토큰이면 멤버 path pull을 위해 JWT/저장소 검증 없이 허용한다")
        void mixerTokenReadsAnyPath() {
            assertThat(service.canRead(MIXER_TOKEN, "path-1-m901")).isTrue();

            verifyNoInteractions(jwtUtil);
            verifyNoInteractions(audioStreamRepository);
        }

        /** 멤버 path "path-1-m{memberId}"의 소유자 행을 만든다. 소속 라이브는 id 1. */
        private StreamMember memberPathOwner(
                Long memberId, Long ownerUserId, StreamStatus streamStatus, StreamMemberStatus ownerStatus
        ) {
            AudioStream stream = StreamFixtures.stream(1L, 10L, 100L, streamStatus);
            StreamMember owner = StreamFixtures.member(
                    memberId, StreamFixtures.bandUser(ownerUserId), stream, ownerStatus);
            owner.assignPath("path-1-m" + memberId);
            return owner;
        }

        @Test
        @DisplayName("같은 라이브의 ACCEPTED 진행자는 다른 진행자의 멤버 path를 WHEP으로 모니터링할 수 있다")
        void acceptedMemberReadsPeerMemberPath() {
            stubValidAccessToken("token", 10L);
            when(streamMemberRepository.findWithUserAndStreamByPath("path-1-m902"))
                    .thenReturn(Optional.of(memberPathOwner(902L, 21L, StreamStatus.OPEN, StreamMemberStatus.ACCEPTED)));
            when(streamMemberRepository.existsByAudioStream_IdAndUser_IdAndStatus(1L, 10L, StreamMemberStatus.ACCEPTED))
                    .thenReturn(true);

            assertThat(service.canRead("token", "path-1-m902")).isTrue();
        }

        @Test
        @DisplayName("[공격] 일반 청취자는 믹싱 전 원본인 멤버 path를 청취할 수 없다")
        void normalUserCannotReadMemberPath() {
            stubValidAccessToken("token", 20L);
            when(streamMemberRepository.findWithUserAndStreamByPath("path-1-m901"))
                    .thenReturn(Optional.of(memberPathOwner(901L, 10L, StreamStatus.OPEN, StreamMemberStatus.ACCEPTED)));
            when(streamMemberRepository.existsByAudioStream_IdAndUser_IdAndStatus(1L, 20L, StreamMemberStatus.ACCEPTED))
                    .thenReturn(false);

            assertThat(service.canRead("token", "path-1-m901")).isFalse();
        }

        @Test
        @DisplayName("[공격] 다른 라이브의 진행자 자격으로는 이 라이브의 멤버 path를 청취할 수 없다")
        void crossStreamMemberCannotReadMemberPath() {
            stubValidAccessToken("token", 30L);
            when(streamMemberRepository.findWithUserAndStreamByPath("path-1-m902"))
                    .thenReturn(Optional.of(memberPathOwner(902L, 21L, StreamStatus.OPEN, StreamMemberStatus.ACCEPTED)));
            // ACCEPTED 여부는 반드시 path가 속한 라이브(id 1) 범위로만 검사되어야 한다
            when(streamMemberRepository.existsByAudioStream_IdAndUser_IdAndStatus(1L, 30L, StreamMemberStatus.ACCEPTED))
                    .thenReturn(false);

            assertThat(service.canRead("token", "path-1-m902")).isFalse();
        }

        @Test
        @DisplayName("[공격] 초대만 받고 수락하지 않은(INVITED) 유저는 멤버 path를 청취할 수 없다")
        void invitedUserCannotReadMemberPath() {
            stubValidAccessToken("token", 40L);
            when(streamMemberRepository.findWithUserAndStreamByPath("path-1-m902"))
                    .thenReturn(Optional.of(memberPathOwner(902L, 21L, StreamStatus.OPEN, StreamMemberStatus.ACCEPTED)));
            // exists 검사가 ACCEPTED로 한정되므로 INVITED/REJECTED는 여기서 걸러진다
            when(streamMemberRepository.existsByAudioStream_IdAndUser_IdAndStatus(1L, 40L, StreamMemberStatus.ACCEPTED))
                    .thenReturn(false);

            assertThat(service.canRead("token", "path-1-m902")).isFalse();
        }

        @Test
        @DisplayName("[공격] 종료(CLOSED)된 라이브의 멤버 path는 진행자였더라도 청취할 수 없다")
        void closedStreamMemberPathRejected() {
            stubValidAccessToken("token", 10L);
            when(streamMemberRepository.findWithUserAndStreamByPath("path-1-m902"))
                    .thenReturn(Optional.of(memberPathOwner(902L, 21L, StreamStatus.CLOSED, StreamMemberStatus.ACCEPTED)));

            assertThat(service.canRead("token", "path-1-m902")).isFalse();
        }

        @Test
        @DisplayName("[공격] path 소유자가 확정 진행자(ACCEPTED)가 아니게 되면 그 path는 청취할 수 없다")
        void demotedOwnerMemberPathRejected() {
            stubValidAccessToken("token", 10L);
            when(streamMemberRepository.findWithUserAndStreamByPath("path-1-m902"))
                    .thenReturn(Optional.of(memberPathOwner(902L, 21L, StreamStatus.OPEN, StreamMemberStatus.REJECTED)));

            assertThat(service.canRead("token", "path-1-m902")).isFalse();
        }

        @Test
        @DisplayName("[공격] 발급된 적 없는 멤버 path 형식을 추측해 요청하면 거부되고, 메인 path 검사로 폴백하지 않는다")
        void guessedMemberPathRejected() {
            stubValidAccessToken("token", 10L);
            when(streamMemberRepository.findWithUserAndStreamByPath("path-1-m999")).thenReturn(Optional.empty());

            assertThat(service.canRead("token", "path-1-m999")).isFalse();
            // 멤버 path 요청이 AudioStream 존재 검사로 새어 나가면 path 유효성 정보가 노출된다
            verify(audioStreamRepository, never()).existsByPathAndStatus(anyString(), any(StreamStatus.class));
        }
    }

    @Nested
    @DisplayName("믹서 토큰 우회 방어")
    class MixerTokenBypassTest {

        /** 믹서 토큰이 미설정(빈 값)인 서버 구성. */
        private StreamServiceImpl blankTokenService() {
            return new StreamServiceImpl(
                    jwtUtil, audioStreamRepository, streamMemberRepository, liveAlarmRepository,
                    streamReplayRepository, reportHistoryRepository, userPort, redisTemplate,
                    bandMemberPort, followPort, userTermsPort, notifyPort, mtxRestClient,
                    viewerSsePresence, liveChatRoomCloser, discordMessageSender,
                    HLS_URL, WEBRTC_URL, ""
            );
        }

        @Test
        @DisplayName("[공격] 토큰 미설정 서버에 빈 password를 보내도 publish/read 인증을 우회할 수 없다")
        void blankPasswordDoesNotBypassWhenTokenUnset() {
            StreamServiceImpl blankService = blankTokenService();
            when(jwtUtil.isValid("")).thenReturn(false);

            assertThat(blankService.canPublish("", "path-1-m901")).isFalse();
            assertThat(blankService.canRead("", "path-1")).isFalse();

            verifyNoInteractions(streamMemberRepository, audioStreamRepository);
        }

        @Test
        @DisplayName("[공격] 공백 문자열 password로도 우회할 수 없다")
        void whitespacePasswordDoesNotBypass() {
            StreamServiceImpl blankService = blankTokenService();
            when(jwtUtil.isValid(" ")).thenReturn(false);

            assertThat(blankService.canRead(" ", "path-1")).isFalse();
        }

        @Test
        @DisplayName("[공격] 틀린 믹서 토큰 값은 JWT 검증 경로로 넘어가 거부된다")
        void wrongMixerTokenFallsThroughToJwt() {
            when(jwtUtil.isValid("wrong-secret")).thenReturn(false);

            assertThat(service.canPublish("wrong-secret", "path-1")).isFalse();
            assertThat(service.canRead("wrong-secret", "path-1-m901")).isFalse();

            verifyNoInteractions(streamMemberRepository, audioStreamRepository);
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
        @DisplayName("커밋 후 진행자 개인 path에 남은 WHIP 세션들도 함께 kick 한다")
        void kicksMemberPublishersAfterCommit() {
            AudioStream stream = StreamFixtures.stream(1L, 10L, 100L, StreamStatus.OPEN);
            when(audioStreamRepository.findById(1L)).thenReturn(Optional.of(stream));
            when(bandMemberPort.isActiveRegularMemberOfBand(100L, 10L)).thenReturn(true);
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.zCard("viewer:1")).thenReturn(0L);

            StreamMember owner = StreamFixtures.member(
                    901L, StreamFixtures.bandUser(10L), stream, StreamMemberStatus.ACCEPTED);
            owner.assignPath("path-1-m901");
            // path 미발급(방에 실제 진입한 적 없는) 멤버는 kick 조회 대상이 아니다
            StreamMember neverJoined = StreamFixtures.member(
                    902L, StreamFixtures.bandUser(21L), stream, StreamMemberStatus.ACCEPTED);
            when(streamMemberRepository.findAllByAudioStream_IdAndStatus(1L, StreamMemberStatus.ACCEPTED))
                    .thenReturn(List.of(owner, neverJoined));

            stubMtxPathLookup("path-1", null);  // 메인 path는 믹서(RTSP) 소스라 조회 응답 없음 → 조기 종료
            stubMtxPathLookup("path-1-m901",
                    new MtxPathResponse(new MtxPathResponse.Source("webRTCSession", "session-9")));
            stubMtxKick("session-9");

            service.closeStream(10L, 1L);
            TxSyncSupport.triggerAfterCommit();

            verify(mtxKickUriSpec).uri("v3/webrtcsessions/kick/{id}", "session-9");
            verify(mtxGetUriSpec, never()).uri("v3/paths/get/{name}", "path-1-m902");
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

    /** enterRoom의 밴드 모드 접근 검증(validAccessAboutStreamInBandMode)을 통과시키는 스텁. */
    private void givenStreamAccessAllowed(Long userId) {
        when(userPort.validAccessAboutStreamInBandMode(eq(userId), any())).thenReturn(true);
    }

    @Nested
    @DisplayName("enterRoom - 밴드 모드 접근 제어")
    class EnterRoomAccessControlTest {

        @Test
        @DisplayName("접근 검증에 실패하면 FORBIDDEN_REQUEST를 던지고, 검증에는 ACCEPTED 진행자만 전달한다")
        void forbiddenWhenAccessDeniedAndOnlyAcceptedCoHostsPassed() {
            AudioStream stream = StreamFixtures.streamWithCoHost(1L, 10L, 100L, StreamStatus.OPEN, List.of(
                    StreamFixtures.member(901L, StreamFixtures.bandUser(10L), null, StreamMemberStatus.ACCEPTED),
                    StreamFixtures.member(902L, StreamFixtures.bandUser(21L), null, StreamMemberStatus.INVITED),
                    StreamFixtures.member(903L, StreamFixtures.bandUser(22L), null, StreamMemberStatus.REJECTED)
            ));
            when(audioStreamRepository.findById(1L)).thenReturn(Optional.of(stream));
            when(userPort.validAccessAboutStreamInBandMode(eq(20L), any())).thenReturn(false);

            assertStreamError(() -> service.enterRoom(20L, 1L), StreamErrorCode.FORBIDDEN_REQUEST);

            // INVITED(미수락)/REJECTED(거절)는 coHost가 아니므로 검증 대상에서 제외된다
            verify(userPort).validAccessAboutStreamInBandMode(eq(20L), coHostIdsCaptor.capture());
            assertThat(coHostIdsCaptor.getValue()).containsExactly(10L);

            verifyNoInteractions(redisTemplate, viewerSsePresence);
        }

        @Test
        @DisplayName("밴드 모드 공동 진행자가 청취자로 입장하면 밴드 이름을 닉네임으로 내려준다")
        void bandModeListenerGetsBandName() {
            when(audioStreamRepository.findById(1L))
                    .thenReturn(Optional.of(StreamFixtures.stream(1L, 10L, 100L, StreamStatus.OPEN)));
            givenStreamAccessAllowed(21L);
            when(userPort.isBandMode(21L)).thenReturn(true);
            when(bandMemberPort.getBandName(100L)).thenReturn("밴드");
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.zCard("viewer:1")).thenReturn(1L);
            when(redisTemplate.hasKey("live:path-1")).thenReturn(true);
            when(bandMemberPort.getBandNameWithBandProfileByBroadcasterId(Set.of(10L))).thenReturn(List.of());

            StreamRoomResponse response = service.enterRoom(21L, 1L);

            assertThat(response.nickname()).isEqualTo("밴드");
            verify(userPort, never()).getFanName(anyLong());
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
            givenStreamAccessAllowed(10L);

            assertStreamError(() -> service.enterRoom(10L, 1L), StreamErrorCode.AUDIO_STREAM_NOT_LIVE);

            verifyNoInteractions(bandMemberPort, redisTemplate);
        }

        @Test
        @DisplayName("CANCELED 라이브에 진입하면 AUDIO_STREAM_NOT_LIVE를 던진다")
        void canceledStreamRejected() {
            when(audioStreamRepository.findById(1L))
                    .thenReturn(Optional.of(StreamFixtures.stream(1L, 10L, 100L, StreamStatus.CANCELED)));
            givenStreamAccessAllowed(10L);

            assertStreamError(() -> service.enterRoom(10L, 1L), StreamErrorCode.AUDIO_STREAM_NOT_LIVE);

            verifyNoInteractions(bandMemberPort, redisTemplate);
        }

        @Test
        @DisplayName("SCHEDULED 라이브 시작 시 활성 밴드가 다르면 FORBIDDEN_REQUEST를 던진다")
        void scheduledStartRequiresActiveBand() {
            when(audioStreamRepository.findById(1L))
                    .thenReturn(Optional.of(StreamFixtures.scheduledStream(1L, 10L, 100L, LocalDateTime.now().plusHours(1))));
            givenStreamAccessAllowed(10L);
            when(bandMemberPort.isActiveRegularMemberOfBand(100L, 10L)).thenReturn(false);

            assertStreamError(() -> service.enterRoom(10L, 1L), StreamErrorCode.FORBIDDEN_REQUEST);

            verify(audioStreamRepository, never()).existsByBroadcasterIdAndStatus(anyLong(), any(StreamStatus.class));
        }

        @Test
        @DisplayName("이미 진행 중인 OPEN 라이브가 있으면 ALREADY_LIVE를 던진다")
        void alreadyLive() {
            when(audioStreamRepository.findById(1L))
                    .thenReturn(Optional.of(StreamFixtures.scheduledStream(1L, 10L, 100L, LocalDateTime.now().plusHours(1))));
            givenStreamAccessAllowed(10L);
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
            givenStreamAccessAllowed(10L);
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
            givenStreamAccessAllowed(10L);
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
            givenStreamAccessAllowed(10L);
            when(bandMemberPort.isActiveRegularMemberOfBand(100L, 10L)).thenReturn(true);
            when(bandMemberPort.getBandName(100L)).thenReturn("밴드");
            when(audioStreamRepository.existsByBroadcasterIdAndStatus(10L, StreamStatus.OPEN)).thenReturn(false);
            when(audioStreamRepository.markStartedIfScheduled(eq(1L), any(LocalDateTime.class))).thenReturn(1);
            stubNotifyRecipients(100L, 10L, 20L, true);
            when(redisTemplate.hasKey("live:path-1")).thenReturn(true);
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.zCard("viewer:1")).thenReturn(3L);
            when(bandMemberPort.getBandNameWithBandProfileByBroadcasterId(Set.of(10L)))
                    .thenReturn(List.of(StreamFixtures.bandInfo(10L, "밴드", "https://cdn.test/band.jpg")));
            // 벌크 UPDATE의 clearAutomatically 이후 개인 송출 path 발급을 위해 멤버를 재조회한다
            when(streamMemberRepository.findWithStreamByLiveIdAndUserId(1L, 10L))
                    .thenReturn(Optional.of(StreamFixtures.member(
                            901L, StreamFixtures.bandUser(10L), opened, StreamMemberStatus.ACCEPTED)));

            StreamRoomResponse response = service.enterRoom(10L, 1L);

            // 1차 조회 + 벌크 UPDATE 이후 재조회
            verify(audioStreamRepository, times(2)).findById(1L);
            assertThat(response.liveId()).isEqualTo(1L);
            assertThat(response.isLive()).isTrue();
            assertThat(response.nickname()).isEqualTo("밴드");
            assertThat(response.viewCount()).isEqualTo(3);
            assertThat(response.bandName()).isEqualTo("밴드");
            assertThat(response.bandProfileImageUrl()).isEqualTo("https://cdn.test/band.jpg");
            // 송출자도 메인 path가 아닌 개인 멤버 path({mainPath}-m{memberId})로 송출한다
            assertThat(response.playback()).isEqualTo(new StreamRoomResponse.Playback(
                    "BROADCASTER", "WHIP", WEBRTC_URL + "/path-1-m901/whip"
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
        @DisplayName("이미 OPEN인 라이브 재입장은 밴드 검증과 시작 처리를 건너뛰고 기존 개인 path를 재사용한다")
        void reentryToOpenLiveSkipsStartFlow() {
            AudioStream stream = StreamFixtures.stream(1L, 10L, 100L, StreamStatus.OPEN);
            when(audioStreamRepository.findById(1L)).thenReturn(Optional.of(stream));
            givenStreamAccessAllowed(10L);
            when(bandMemberPort.getBandName(100L)).thenReturn("밴드");
            when(redisTemplate.hasKey("live:path-1")).thenReturn(true);
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.zCard("viewer:1")).thenReturn(1L);
            when(bandMemberPort.getBandNameWithBandProfileByBroadcasterId(Set.of(10L))).thenReturn(List.of());

            // 이미 발급된 path는 재발급 없이 그대로 재사용된다 (재접속 시 MediaMTX가 좀비 세션을 대체)
            StreamMember owner = StreamFixtures.member(
                    901L, StreamFixtures.bandUser(10L), stream, StreamMemberStatus.ACCEPTED);
            owner.assignPath("path-1-m901");
            when(streamMemberRepository.findWithStreamByLiveIdAndUserId(1L, 10L))
                    .thenReturn(Optional.of(owner));

            StreamRoomResponse response = service.enterRoom(10L, 1L);

            verify(audioStreamRepository, times(1)).findById(1L);
            verify(bandMemberPort, never()).isActiveRegularMemberOfBand(anyLong(), anyLong());
            verify(audioStreamRepository, never()).existsByBroadcasterIdAndStatus(anyLong(), any(StreamStatus.class));
            verify(audioStreamRepository, never()).markStartedIfScheduled(anyLong(), any(LocalDateTime.class));
            assertThat(TxSyncSupport.registeredCount()).isZero();
            // 재입장에서도 송출자 닉네임은 밴드 이름
            assertThat(response.nickname()).isEqualTo("밴드");
            assertThat(response.playback().role()).isEqualTo("BROADCASTER");
            assertThat(response.playback().playbackUrl()).isEqualTo(WEBRTC_URL + "/path-1-m901/whip");
            // 다른 진행자가 없으면 WHEP 목록은 빈 리스트 (청취자의 null과 구분되는 진행자 표식)
            assertThat(response.coPublishers()).isEmpty();
        }

        @Test
        @DisplayName("송출자의 ACCEPTED StreamMember 행이 없으면 FORBIDDEN_REQUEST를 던진다")
        void missingOwnerMemberRowRejected() {
            when(audioStreamRepository.findById(1L))
                    .thenReturn(Optional.of(StreamFixtures.stream(1L, 10L, 100L, StreamStatus.OPEN)));
            givenStreamAccessAllowed(10L);
            when(bandMemberPort.getBandName(100L)).thenReturn("밴드");
            when(redisTemplate.hasKey("live:path-1")).thenReturn(true);
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.zCard("viewer:1")).thenReturn(1L);
            when(bandMemberPort.getBandNameWithBandProfileByBroadcasterId(Set.of(10L))).thenReturn(List.of());
            when(streamMemberRepository.findWithStreamByLiveIdAndUserId(1L, 10L)).thenReturn(Optional.empty());

            assertStreamError(() -> service.enterRoom(10L, 1L), StreamErrorCode.FORBIDDEN_REQUEST);
        }
    }

    @Nested
    @DisplayName("enterRoom - 공동 진행자 진입")
    class EnterRoomCoHostTest {

        /** user 21이 ACCEPTED 공동 진행자인 OPEN 라이브. */
        private AudioStream openStreamWithCoHost(StreamMember coHost) {
            return StreamFixtures.streamWithCoHost(1L, 10L, 100L, StreamStatus.OPEN, List.of(
                    StreamFixtures.member(901L, StreamFixtures.bandUser(10L), null, StreamMemberStatus.ACCEPTED),
                    coHost
            ));
        }

        @Test
        @DisplayName("ACCEPTED 공동 진행자는 청취자가 아니라 개인 멤버 path의 WHIP 송출 정보를 받는다")
        void acceptedCoHostGetsWhipPlayback() {
            StreamMember coHost = StreamFixtures.member(
                    902L, StreamFixtures.bandUser(21L), null, StreamMemberStatus.ACCEPTED);
            AudioStream stream = openStreamWithCoHost(coHost);
            when(audioStreamRepository.findById(1L)).thenReturn(Optional.of(stream));
            givenStreamAccessAllowed(21L);
            when(bandMemberPort.getBandName(100L)).thenReturn("밴드");
            when(redisTemplate.hasKey("live:path-1")).thenReturn(true);
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.zCard("viewer:1")).thenReturn(2L);
            when(bandMemberPort.getBandNameWithBandProfileByBroadcasterId(Set.of(10L))).thenReturn(List.of());
            when(streamMemberRepository.findWithStreamByLiveIdAndUserId(1L, 21L))
                    .thenReturn(Optional.of(StreamFixtures.member(
                            902L, StreamFixtures.bandUser(21L), stream, StreamMemberStatus.ACCEPTED)));

            StreamRoomResponse response = service.enterRoom(21L, 1L);

            assertThat(response.nickname()).isEqualTo("밴드");
            assertThat(response.playback()).isEqualTo(new StreamRoomResponse.Playback(
                    "CO_HOST", "WHIP", WEBRTC_URL + "/path-1-m902/whip"
            ));

            // 공동 진행자는 시청자 ZSet에 등록되지 않는다
            verify(zSetOperations, never()).add(anyString(), anyString(), org.mockito.ArgumentMatchers.anyDouble());
            verify(viewerSsePresence, never()).broadcastCount(anyLong());
        }

        @Test
        @DisplayName("진행자 응답에는 본인을 제외한, path가 발급된 다른 진행자들의 WHEP 정보만 담긴다")
        void publisherReceivesPeerWhepUrls() {
            StreamMember coHost = StreamFixtures.member(
                    902L, StreamFixtures.bandUser(21L), null, StreamMemberStatus.ACCEPTED);
            AudioStream stream = openStreamWithCoHost(coHost);
            when(audioStreamRepository.findById(1L)).thenReturn(Optional.of(stream));
            givenStreamAccessAllowed(21L);
            when(bandMemberPort.getBandName(100L)).thenReturn("밴드");
            when(redisTemplate.hasKey("live:path-1")).thenReturn(true);
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.zCard("viewer:1")).thenReturn(2L);
            when(bandMemberPort.getBandNameWithBandProfileByBroadcasterId(Set.of(10L))).thenReturn(List.of());
            when(streamMemberRepository.findWithStreamByLiveIdAndUserId(1L, 21L))
                    .thenReturn(Optional.of(StreamFixtures.member(
                            902L, StreamFixtures.bandUser(21L), stream, StreamMemberStatus.ACCEPTED)));

            // 송출자(path 발급됨) + 본인(제외 대상) + 아직 입장 안 한 멤버(path 없음, 제외 대상)
            StreamMember owner = StreamFixtures.member(
                    901L, StreamFixtures.bandUser(10L), stream, StreamMemberStatus.ACCEPTED);
            owner.assignPath("path-1-m901");
            StreamMember self = StreamFixtures.member(
                    902L, StreamFixtures.bandUser(21L), stream, StreamMemberStatus.ACCEPTED);
            self.assignPath("path-1-m902");
            StreamMember notEntered = StreamFixtures.member(
                    903L, StreamFixtures.bandUser(22L), stream, StreamMemberStatus.ACCEPTED);
            when(streamMemberRepository.findAllByAudioStream_IdAndStatus(1L, StreamMemberStatus.ACCEPTED))
                    .thenReturn(List.of(owner, self, notEntered));

            StreamRoomResponse response = service.enterRoom(21L, 1L);

            assertThat(response.coPublishers()).containsExactly(
                    new StreamRoomResponse.CoPublisher(10L, WEBRTC_URL + "/path-1-m901/whep")
            );
        }

        @Test
        @DisplayName("최초 입장으로 path가 새로 발급되면 커밋 후에만 다른 진행자 전원에게 합류 이벤트를 보낸다")
        void firstJoinNotifiesPeersAfterCommit() {
            StreamMember coHost = StreamFixtures.member(
                    902L, StreamFixtures.bandUser(21L), null, StreamMemberStatus.ACCEPTED);
            AudioStream stream = openStreamWithCoHost(coHost);
            when(audioStreamRepository.findById(1L)).thenReturn(Optional.of(stream));
            givenStreamAccessAllowed(21L);
            when(bandMemberPort.getBandName(100L)).thenReturn("밴드");
            when(redisTemplate.hasKey("live:path-1")).thenReturn(true);
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.zCard("viewer:1")).thenReturn(2L);
            when(bandMemberPort.getBandNameWithBandProfileByBroadcasterId(Set.of(10L))).thenReturn(List.of());
            // path 미발급 상태로 조회 → 최초 입장
            when(streamMemberRepository.findWithStreamByLiveIdAndUserId(1L, 21L))
                    .thenReturn(Optional.of(StreamFixtures.member(
                            902L, StreamFixtures.bandUser(21L), stream, StreamMemberStatus.ACCEPTED)));

            StreamMember owner = StreamFixtures.member(
                    901L, StreamFixtures.bandUser(10L), stream, StreamMemberStatus.ACCEPTED);
            owner.assignPath("path-1-m901");
            StreamMember self = StreamFixtures.member(
                    902L, StreamFixtures.bandUser(21L), stream, StreamMemberStatus.ACCEPTED);
            StreamMember notEntered = StreamFixtures.member(
                    903L, StreamFixtures.bandUser(22L), stream, StreamMemberStatus.ACCEPTED);
            when(streamMemberRepository.findAllByAudioStream_IdAndStatus(1L, StreamMemberStatus.ACCEPTED))
                    .thenReturn(List.of(owner, self, notEntered));

            service.enterRoom(21L, 1L);

            // 커밋 전에는 절대 전송하지 않는다 (path 저장 전에 FE가 WHEP을 시도하면 인가에 실패한다)
            verify(viewerSsePresence, never())
                    .notifyCoPublisherJoined(anyLong(), any(), any(StreamRoomResponse.CoPublisher.class));

            TxSyncSupport.triggerAfterCommit();

            // 대상: 본인(21L)을 제외한 ACCEPTED 멤버 전원 (path 미발급이어도 SSE만 연결해 둔 진행자가 받을 수 있어야 함)
            verify(viewerSsePresence).notifyCoPublisherJoined(
                    1L,
                    List.of(10L, 22L),
                    new StreamRoomResponse.CoPublisher(21L, WEBRTC_URL + "/path-1-m902/whep")
            );
        }

        @Test
        @DisplayName("재입장(path 기존 보유)에는 합류 이벤트를 보내지 않는다")
        void reentryDoesNotNotifyPeers() {
            StreamMember coHost = StreamFixtures.member(
                    902L, StreamFixtures.bandUser(21L), null, StreamMemberStatus.ACCEPTED);
            AudioStream stream = openStreamWithCoHost(coHost);
            when(audioStreamRepository.findById(1L)).thenReturn(Optional.of(stream));
            givenStreamAccessAllowed(21L);
            when(bandMemberPort.getBandName(100L)).thenReturn("밴드");
            when(redisTemplate.hasKey("live:path-1")).thenReturn(true);
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.zCard("viewer:1")).thenReturn(2L);
            when(bandMemberPort.getBandNameWithBandProfileByBroadcasterId(Set.of(10L))).thenReturn(List.of());
            // 이미 path를 보유한 상태로 재입장
            StreamMember reentering = StreamFixtures.member(
                    902L, StreamFixtures.bandUser(21L), stream, StreamMemberStatus.ACCEPTED);
            reentering.assignPath("path-1-m902");
            when(streamMemberRepository.findWithStreamByLiveIdAndUserId(1L, 21L))
                    .thenReturn(Optional.of(reentering));

            service.enterRoom(21L, 1L);
            TxSyncSupport.triggerAfterCommit();

            verify(viewerSsePresence, never())
                    .notifyCoPublisherJoined(anyLong(), any(), any(StreamRoomResponse.CoPublisher.class));
        }

        @Test
        @DisplayName("OPEN이 아닌 라이브에는 공동 진행자도 진입할 수 없다 (방송 시작 권한은 송출자 전용)")
        void coHostCannotEnterNotOpenStream() {
            StreamMember coHost = StreamFixtures.member(
                    902L, StreamFixtures.bandUser(21L), null, StreamMemberStatus.ACCEPTED);
            AudioStream scheduled = StreamFixtures.streamWithCoHost(
                    1L, 10L, 100L, StreamStatus.SCHEDULED, List.of(coHost));
            when(audioStreamRepository.findById(1L)).thenReturn(Optional.of(scheduled));
            givenStreamAccessAllowed(21L);

            assertStreamError(() -> service.enterRoom(21L, 1L), StreamErrorCode.AUDIO_STREAM_NOT_LIVE);

            verify(audioStreamRepository, never()).markStartedIfScheduled(anyLong(), any(LocalDateTime.class));
            verifyNoInteractions(redisTemplate, viewerSsePresence);
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
            givenStreamAccessAllowed(20L);

            assertStreamError(() -> service.enterRoom(20L, 1L), StreamErrorCode.AUDIO_STREAM_NOT_LIVE);

            verifyNoInteractions(redisTemplate, viewerSsePresence);
        }

        @Test
        @DisplayName("진입 시 시청자 ZSet에 현재 epoch초 점수로 등록하고 카운트를 브로드캐스트한다")
        void registersViewerAndBroadcasts() {
            when(audioStreamRepository.findById(1L))
                    .thenReturn(Optional.of(StreamFixtures.stream(1L, 10L, 100L, StreamStatus.OPEN)));
            givenStreamAccessAllowed(20L);
            when(userPort.isBandMode(20L)).thenReturn(false);
            when(userPort.getFanName(20L)).thenReturn("팬닉네임");
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
            // 팬 모드 청취자는 FanProfile.nickname을 노출
            assertThat(response.nickname()).isEqualTo("팬닉네임");
            assertThat(response.viewCount()).isEqualTo(5);
            assertThat(response.playback()).isEqualTo(new StreamRoomResponse.Playback(
                    "LISTENER", "HLS", HLS_URL + "/path-1/index.m3u8"
            ));
            // [공격 방어] 청취자에게 멤버 path가 유추될 정보(coPublishers)를 노출하지 않는다
            assertThat(response.coPublishers()).isNull();
            verify(streamMemberRepository, never())
                    .findAllByAudioStream_IdAndStatus(anyLong(), any(StreamMemberStatus.class));
            // 청취자 입장은 진행자 합류 이벤트도 발생시키지 않는다
            TxSyncSupport.triggerAfterCommit();
            verify(viewerSsePresence, never())
                    .notifyCoPublisherJoined(anyLong(), any(), any(StreamRoomResponse.CoPublisher.class));
        }

        @Test
        @DisplayName("Redis 라이브 키가 없으면 isLive=false, playback은 null이다")
        void noLiveKeyMeansNullPlayback() {
            when(audioStreamRepository.findById(1L))
                    .thenReturn(Optional.of(StreamFixtures.stream(1L, 10L, 100L, StreamStatus.OPEN)));
            givenStreamAccessAllowed(20L);
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
            givenStreamAccessAllowed(20L);
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
