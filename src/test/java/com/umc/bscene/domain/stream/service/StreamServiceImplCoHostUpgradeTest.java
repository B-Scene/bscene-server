package com.umc.bscene.domain.stream.service;

import com.umc.bscene.domain.chat.service.LiveChatRoomCloser;
import com.umc.bscene.domain.stream.dto.response.CoHostUpgradeEvent;
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
import com.umc.bscene.domain.stream.repository.AudioStreamRepository;
import com.umc.bscene.domain.stream.repository.LiveAlarmRepository;
import com.umc.bscene.domain.stream.repository.ReportHistoryRepository;
import com.umc.bscene.domain.stream.repository.StreamMemberRepository;
import com.umc.bscene.domain.stream.repository.StreamReplayRepository;
import com.umc.bscene.domain.stream.sse.ViewerSsePresence;
import com.umc.bscene.domain.user.entity.User;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 공동 송출자 업그레이드 요청/수락 단위 테스트.
 * <p>
 * 요청·수락 모두 모달 기반 실시간 흐름이므로 SSE 타겟 전송(송출자/요청자에게만)과
 * 커밋 이후 전송 계약, 그리고 해커 관점의 권한 우회 시나리오를 함께 고정한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StreamServiceImpl 공동 송출자 업그레이드")
class StreamServiceImplCoHostUpgradeTest {

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
    @Mock private NotifyPort notifyPort;
    @Mock private RestClient mtxRestClient;
    @Mock private ViewerSsePresence viewerSsePresence;
    @Mock private LiveChatRoomCloser liveChatRoomCloser;
    @Mock private DiscordMessageSender discordMessageSender;

    private StreamServiceImpl service;

    private static final Long LIVE_ID = 1L;
    private static final Long BROADCASTER_ID = 10L;
    private static final Long BAND_ID = 100L;
    private static final Long REQUESTER_ID = 20L;

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
                notifyPort,
                mtxRestClient,
                viewerSsePresence,
                liveChatRoomCloser,
                discordMessageSender,
                HLS_URL,
                WEBRTC_URL,
                "mixer-secret"
        );
        TxSyncSupport.begin();
    }

    @AfterEach
    void tearDown() {
        TxSyncSupport.end();
    }

    private static void assertStreamError(ThrowingCallable callable, StreamErrorCode expected) {
        assertThatThrownBy(callable)
                .isInstanceOf(StreamException.class)
                .hasFieldOrPropertyWithValue("baseResponseCode", expected);
    }

    private AudioStream openStream() {
        return StreamFixtures.stream(LIVE_ID, BROADCASTER_ID, BAND_ID, StreamStatus.OPEN);
    }

    /** liveId 1의 requester 멤버 행. */
    private StreamMember requesterRow(AudioStream stream, StreamMemberStatus status) {
        return StreamFixtures.member(902L, StreamFixtures.bandUser(REQUESTER_ID), stream, status);
    }

    private void givenOpenStreamAndBandMember() {
        when(audioStreamRepository.findById(LIVE_ID)).thenReturn(Optional.of(openStream()));
        when(bandMemberPort.isActiveRegularMemberOfBand(BAND_ID, REQUESTER_ID)).thenReturn(true);
    }

    @Nested
    @DisplayName("requestCoHostUpgrade - 업그레이드 요청")
    class RequestTest {

        @Test
        @DisplayName("[공격] 팬 모드 유저는 요청 자체가 차단되고 어떤 저장소도 조회하지 않는다")
        void fanModeRejected() {
            User fan = StreamFixtures.fanUser(REQUESTER_ID);

            assertStreamError(() -> service.requestCoHostUpgrade(fan, LIVE_ID), StreamErrorCode.FORBIDDEN_REQUEST);

            verifyNoInteractions(audioStreamRepository, streamMemberRepository, viewerSsePresence);
        }

        @Test
        @DisplayName("라이브가 없으면 AUDIO_STREAM_NOT_FOUND를 던진다")
        void notFound() {
            when(audioStreamRepository.findById(LIVE_ID)).thenReturn(Optional.empty());

            assertStreamError(
                    () -> service.requestCoHostUpgrade(StreamFixtures.bandUser(REQUESTER_ID), LIVE_ID),
                    StreamErrorCode.AUDIO_STREAM_NOT_FOUND
            );
        }

        @Test
        @DisplayName("[공격] 라이브 밴드의 정회원이 아니면 상태 정보를 열거하기 전에 FORBIDDEN으로 차단된다")
        void outsiderRejectedBeforeStateChecks() {
            when(audioStreamRepository.findById(LIVE_ID)).thenReturn(Optional.of(openStream()));
            when(bandMemberPort.isActiveRegularMemberOfBand(BAND_ID, REQUESTER_ID)).thenReturn(false);

            assertStreamError(
                    () -> service.requestCoHostUpgrade(StreamFixtures.bandUser(REQUESTER_ID), LIVE_ID),
                    StreamErrorCode.FORBIDDEN_REQUEST
            );

            // 권한 없는 유저에게 멤버십/요청 상태가 노출되면 안 된다
            verifyNoInteractions(streamMemberRepository);
        }

        @Test
        @DisplayName("OPEN이 아닌 라이브에는 업그레이드를 요청할 수 없다")
        void notOpenRejected() {
            when(audioStreamRepository.findById(LIVE_ID))
                    .thenReturn(Optional.of(StreamFixtures.stream(LIVE_ID, BROADCASTER_ID, BAND_ID, StreamStatus.SCHEDULED)));
            when(bandMemberPort.isActiveRegularMemberOfBand(BAND_ID, REQUESTER_ID)).thenReturn(true);

            assertStreamError(
                    () -> service.requestCoHostUpgrade(StreamFixtures.bandUser(REQUESTER_ID), LIVE_ID),
                    StreamErrorCode.AUDIO_STREAM_NOT_LIVE
            );

            verifyNoInteractions(streamMemberRepository);
        }

        @Test
        @DisplayName("[공격] 이미 확정(ACCEPTED)된 진행자(송출자 본인 포함)의 중복 요청은 409로 거부된다")
        void alreadyAcceptedRejected() {
            AudioStream stream = openStream();
            givenOpenStreamAndBandMember();
            when(streamMemberRepository.findWithStreamByLiveIdAndUserId(LIVE_ID, REQUESTER_ID))
                    .thenReturn(Optional.of(requesterRow(stream, StreamMemberStatus.ACCEPTED)));

            assertStreamError(
                    () -> service.requestCoHostUpgrade(StreamFixtures.bandUser(REQUESTER_ID), LIVE_ID),
                    StreamErrorCode.CO_HOST_ALREADY_ACCEPTED
            );

            verify(streamMemberRepository, never()).save(any(StreamMember.class));
            assertThat(TxSyncSupport.registeredCount()).isZero();
        }

        @Test
        @DisplayName("[공격] 처리 대기 중(REQUESTED) 요청이 있으면 중복 요청은 409로 거부된다")
        void duplicateRequestRejected() {
            AudioStream stream = openStream();
            givenOpenStreamAndBandMember();
            when(streamMemberRepository.findWithStreamByLiveIdAndUserId(LIVE_ID, REQUESTER_ID))
                    .thenReturn(Optional.of(requesterRow(stream, StreamMemberStatus.REQUESTED)));

            assertStreamError(
                    () -> service.requestCoHostUpgrade(StreamFixtures.bandUser(REQUESTER_ID), LIVE_ID),
                    StreamErrorCode.CO_HOST_UPGRADE_ALREADY_REQUESTED
            );

            verify(streamMemberRepository, never())
                    .transitionStatus(anyLong(), any(StreamMemberStatus.class), any(StreamMemberStatus.class));
            assertThat(TxSyncSupport.registeredCount()).isZero();
        }

        @Test
        @DisplayName("수락 전 초대(INVITED) 행이 있으면 원자 전이로 REQUESTED로 바뀐다")
        void invitedRowTransitionsToRequested() {
            AudioStream stream = openStream();
            givenOpenStreamAndBandMember();
            when(streamMemberRepository.findWithStreamByLiveIdAndUserId(LIVE_ID, REQUESTER_ID))
                    .thenReturn(Optional.of(requesterRow(stream, StreamMemberStatus.INVITED)));
            when(streamMemberRepository.transitionStatus(902L, StreamMemberStatus.INVITED, StreamMemberStatus.REQUESTED))
                    .thenReturn(1);
            when(bandMemberPort.getBandMemberNickname(BAND_ID, REQUESTER_ID)).thenReturn(Optional.of("닉네임"));

            service.requestCoHostUpgrade(StreamFixtures.bandUser(REQUESTER_ID), LIVE_ID);

            verify(streamMemberRepository, never()).save(any(StreamMember.class));
            verify(streamMemberRepository).transitionStatus(902L, StreamMemberStatus.INVITED, StreamMemberStatus.REQUESTED);
        }

        @Test
        @DisplayName("거절(REJECTED)된 적 있는 멤버도 라이브 중에는 원자 전이로 다시 요청할 수 있다")
        void rejectedRowTransitionsToRequested() {
            AudioStream stream = openStream();
            givenOpenStreamAndBandMember();
            when(streamMemberRepository.findWithStreamByLiveIdAndUserId(LIVE_ID, REQUESTER_ID))
                    .thenReturn(Optional.of(requesterRow(stream, StreamMemberStatus.REJECTED)));
            when(streamMemberRepository.transitionStatus(902L, StreamMemberStatus.REJECTED, StreamMemberStatus.REQUESTED))
                    .thenReturn(1);
            when(bandMemberPort.getBandMemberNickname(BAND_ID, REQUESTER_ID)).thenReturn(Optional.of("닉네임"));

            service.requestCoHostUpgrade(StreamFixtures.bandUser(REQUESTER_ID), LIVE_ID);

            verify(streamMemberRepository).transitionStatus(902L, StreamMemberStatus.REJECTED, StreamMemberStatus.REQUESTED);
        }

        @Test
        @DisplayName("[공격] 상태 전이가 경합에서 지면(0건) CO_HOST_CONFLICT를 던지고 SSE 훅을 등록하지 않는다")
        void transitionRaceLost() {
            AudioStream stream = openStream();
            givenOpenStreamAndBandMember();
            when(streamMemberRepository.findWithStreamByLiveIdAndUserId(LIVE_ID, REQUESTER_ID))
                    .thenReturn(Optional.of(requesterRow(stream, StreamMemberStatus.INVITED)));
            when(streamMemberRepository.transitionStatus(902L, StreamMemberStatus.INVITED, StreamMemberStatus.REQUESTED))
                    .thenReturn(0);

            assertStreamError(
                    () -> service.requestCoHostUpgrade(StreamFixtures.bandUser(REQUESTER_ID), LIVE_ID),
                    StreamErrorCode.CO_HOST_CONFLICT
            );

            assertThat(TxSyncSupport.registeredCount()).isZero();
        }

        @Test
        @DisplayName("신규 요청은 REQUESTED로 저장되고, 커밋 후에만 송출자에게 수락 모달용 SSE를 보낸다")
        void freshRequestSavesAndNotifiesOwnerAfterCommit() {
            givenOpenStreamAndBandMember();
            when(streamMemberRepository.findWithStreamByLiveIdAndUserId(LIVE_ID, REQUESTER_ID))
                    .thenReturn(Optional.empty());
            when(bandMemberPort.getBandMemberNickname(BAND_ID, REQUESTER_ID)).thenReturn(Optional.of("닉네임"));

            User requester = StreamFixtures.bandUser(REQUESTER_ID);
            service.requestCoHostUpgrade(requester, LIVE_ID);

            ArgumentCaptor<StreamMember> captor = ArgumentCaptor.forClass(StreamMember.class);
            verify(streamMemberRepository).save(captor.capture());
            assertThat(captor.getValue().getUser()).isSameAs(requester);
            assertThat(captor.getValue().getStatus()).isEqualTo(StreamMemberStatus.REQUESTED);

            // 송출자는 방송 이탈이 불가하므로 SSE 모달로 전달한다. 단, path 커밋 전 전송 금지
            verify(viewerSsePresence, never())
                    .notifyCoHostUpgradeRequested(anyLong(), anyLong(), any(CoHostUpgradeEvent.class));

            TxSyncSupport.triggerAfterCommit();

            verify(viewerSsePresence).notifyCoHostUpgradeRequested(
                    LIVE_ID, BROADCASTER_ID, new CoHostUpgradeEvent(REQUESTER_ID, "닉네임"));
        }

        @Test
        @DisplayName("[공격] 동시 중복 insert는 unique 제약(user, stream)에 걸려 CO_HOST_CONFLICT로 변환된다")
        void concurrentDuplicateInsertRejected() {
            givenOpenStreamAndBandMember();
            when(streamMemberRepository.findWithStreamByLiveIdAndUserId(LIVE_ID, REQUESTER_ID))
                    .thenReturn(Optional.empty());
            when(streamMemberRepository.save(any(StreamMember.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicate (user, stream)"));

            assertStreamError(
                    () -> service.requestCoHostUpgrade(StreamFixtures.bandUser(REQUESTER_ID), LIVE_ID),
                    StreamErrorCode.CO_HOST_CONFLICT
            );

            assertThat(TxSyncSupport.registeredCount()).isZero();
        }
    }

    @Nested
    @DisplayName("acceptCoHostUpgrade - 업그레이드 수락")
    class AcceptTest {

        @Test
        @DisplayName("라이브가 없으면 AUDIO_STREAM_NOT_FOUND를 던진다")
        void notFound() {
            when(audioStreamRepository.findById(LIVE_ID)).thenReturn(Optional.empty());

            assertStreamError(
                    () -> service.acceptCoHostUpgrade(BROADCASTER_ID, LIVE_ID, REQUESTER_ID),
                    StreamErrorCode.AUDIO_STREAM_NOT_FOUND
            );
        }

        @Test
        @DisplayName("[공격] 송출자(오너)가 아니면 공동 진행자여도 수락할 수 없고 요청 존재 여부도 열거되지 않는다")
        void nonOwnerCannotAccept() {
            when(audioStreamRepository.findById(LIVE_ID)).thenReturn(Optional.of(openStream()));

            assertStreamError(
                    () -> service.acceptCoHostUpgrade(21L, LIVE_ID, REQUESTER_ID),
                    StreamErrorCode.FORBIDDEN_REQUEST
            );

            verifyNoInteractions(streamMemberRepository, viewerSsePresence);
        }

        @Test
        @DisplayName("[공격] 타 밴드 프로필로 전환한 송출자는 수락할 수 없다")
        void ownerWithSwitchedBandProfileRejected() {
            when(audioStreamRepository.findById(LIVE_ID)).thenReturn(Optional.of(openStream()));
            when(bandMemberPort.isActiveRegularMemberOfBand(BAND_ID, BROADCASTER_ID)).thenReturn(false);

            assertStreamError(
                    () -> service.acceptCoHostUpgrade(BROADCASTER_ID, LIVE_ID, REQUESTER_ID),
                    StreamErrorCode.FORBIDDEN_REQUEST
            );

            verifyNoInteractions(streamMemberRepository);
        }

        @Test
        @DisplayName("OPEN이 아닌 라이브에서는 수락할 수 없다")
        void notOpenRejected() {
            when(audioStreamRepository.findById(LIVE_ID))
                    .thenReturn(Optional.of(StreamFixtures.stream(LIVE_ID, BROADCASTER_ID, BAND_ID, StreamStatus.CLOSED)));
            when(bandMemberPort.isActiveRegularMemberOfBand(BAND_ID, BROADCASTER_ID)).thenReturn(true);

            assertStreamError(
                    () -> service.acceptCoHostUpgrade(BROADCASTER_ID, LIVE_ID, REQUESTER_ID),
                    StreamErrorCode.AUDIO_STREAM_NOT_LIVE
            );
        }

        @Test
        @DisplayName("요청 행이 없으면 CO_HOST_UPGRADE_REQUEST_NOT_FOUND를 던진다")
        void requestRowMissing() {
            when(audioStreamRepository.findById(LIVE_ID)).thenReturn(Optional.of(openStream()));
            when(bandMemberPort.isActiveRegularMemberOfBand(BAND_ID, BROADCASTER_ID)).thenReturn(true);
            when(streamMemberRepository.findWithStreamByLiveIdAndUserId(LIVE_ID, REQUESTER_ID))
                    .thenReturn(Optional.empty());

            assertStreamError(
                    () -> service.acceptCoHostUpgrade(BROADCASTER_ID, LIVE_ID, REQUESTER_ID),
                    StreamErrorCode.CO_HOST_UPGRADE_REQUEST_NOT_FOUND
            );
        }

        @Test
        @DisplayName("[공격] INVITED 행을 이 API로 수락해 초대 수락 플로우를 우회할 수 없다")
        void invitedRowCannotBeAcceptedViaUpgradeApi() {
            AudioStream stream = openStream();
            when(audioStreamRepository.findById(LIVE_ID)).thenReturn(Optional.of(stream));
            when(bandMemberPort.isActiveRegularMemberOfBand(BAND_ID, BROADCASTER_ID)).thenReturn(true);
            when(streamMemberRepository.findWithStreamByLiveIdAndUserId(LIVE_ID, REQUESTER_ID))
                    .thenReturn(Optional.of(requesterRow(stream, StreamMemberStatus.INVITED)));

            assertStreamError(
                    () -> service.acceptCoHostUpgrade(BROADCASTER_ID, LIVE_ID, REQUESTER_ID),
                    StreamErrorCode.CO_HOST_UPGRADE_REQUEST_NOT_FOUND
            );

            verify(streamMemberRepository, never())
                    .transitionStatus(anyLong(), any(StreamMemberStatus.class), any(StreamMemberStatus.class));
        }

        @Test
        @DisplayName("이미 확정(ACCEPTED)된 멤버의 요청 수락은 409로 거부된다")
        void alreadyAcceptedRejected() {
            AudioStream stream = openStream();
            when(audioStreamRepository.findById(LIVE_ID)).thenReturn(Optional.of(stream));
            when(bandMemberPort.isActiveRegularMemberOfBand(BAND_ID, BROADCASTER_ID)).thenReturn(true);
            when(streamMemberRepository.findWithStreamByLiveIdAndUserId(LIVE_ID, REQUESTER_ID))
                    .thenReturn(Optional.of(requesterRow(stream, StreamMemberStatus.ACCEPTED)));

            assertStreamError(
                    () -> service.acceptCoHostUpgrade(BROADCASTER_ID, LIVE_ID, REQUESTER_ID),
                    StreamErrorCode.CO_HOST_ALREADY_ACCEPTED
            );
        }

        @Test
        @DisplayName("수락 시 REQUESTED→ACCEPTED 원자 전이 후, 커밋 후에만 요청자에게 SSE를 보낸다")
        void acceptTransitionsAndNotifiesRequesterAfterCommit() {
            AudioStream stream = openStream();
            when(audioStreamRepository.findById(LIVE_ID)).thenReturn(Optional.of(stream));
            when(bandMemberPort.isActiveRegularMemberOfBand(BAND_ID, BROADCASTER_ID)).thenReturn(true);
            when(streamMemberRepository.findWithStreamByLiveIdAndUserId(LIVE_ID, REQUESTER_ID))
                    .thenReturn(Optional.of(requesterRow(stream, StreamMemberStatus.REQUESTED)));
            when(streamMemberRepository.transitionStatus(902L, StreamMemberStatus.REQUESTED, StreamMemberStatus.ACCEPTED))
                    .thenReturn(1);
            when(bandMemberPort.getBandMemberNickname(BAND_ID, REQUESTER_ID)).thenReturn(Optional.of("닉네임"));

            service.acceptCoHostUpgrade(BROADCASTER_ID, LIVE_ID, REQUESTER_ID);

            verify(streamMemberRepository).transitionStatus(902L, StreamMemberStatus.REQUESTED, StreamMemberStatus.ACCEPTED);

            // 요청자는 수신 즉시 enterRoom을 재호출해 송출 정보를 받는다. 단, 커밋 전 전송 금지
            verify(viewerSsePresence, never())
                    .notifyCoHostUpgradeAccepted(anyLong(), anyLong(), any(CoHostUpgradeEvent.class));

            TxSyncSupport.triggerAfterCommit();

            verify(viewerSsePresence).notifyCoHostUpgradeAccepted(
                    LIVE_ID, REQUESTER_ID, new CoHostUpgradeEvent(REQUESTER_ID, "닉네임"));
        }

        @Test
        @DisplayName("[공격] 전이가 경합에서 지면(0건) CO_HOST_CONFLICT를 던지고 SSE 훅을 등록하지 않는다")
        void transitionRaceLost() {
            AudioStream stream = openStream();
            when(audioStreamRepository.findById(LIVE_ID)).thenReturn(Optional.of(stream));
            when(bandMemberPort.isActiveRegularMemberOfBand(BAND_ID, BROADCASTER_ID)).thenReturn(true);
            when(streamMemberRepository.findWithStreamByLiveIdAndUserId(LIVE_ID, REQUESTER_ID))
                    .thenReturn(Optional.of(requesterRow(stream, StreamMemberStatus.REQUESTED)));
            when(streamMemberRepository.transitionStatus(902L, StreamMemberStatus.REQUESTED, StreamMemberStatus.ACCEPTED))
                    .thenReturn(0);

            assertStreamError(
                    () -> service.acceptCoHostUpgrade(BROADCASTER_ID, LIVE_ID, REQUESTER_ID),
                    StreamErrorCode.CO_HOST_CONFLICT
            );

            assertThat(TxSyncSupport.registeredCount()).isZero();
        }
    }
}
