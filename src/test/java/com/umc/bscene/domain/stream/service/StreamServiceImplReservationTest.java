package com.umc.bscene.domain.stream.service;

import com.umc.bscene.domain.chat.service.LiveChatRoomCloser;
import com.umc.bscene.domain.stream.dto.CoHostCandidateInfo;
import com.umc.bscene.domain.stream.dto.request.ReportUserRequest;
import com.umc.bscene.domain.stream.dto.request.ReservationPatchRequest;
import com.umc.bscene.domain.stream.dto.response.CoHostCandidateResponse;
import com.umc.bscene.domain.stream.dto.response.LiveMembersResponse;
import com.umc.bscene.domain.stream.dto.response.ReservationEditResponse;
import com.umc.bscene.domain.stream.entity.AudioStream;
import com.umc.bscene.domain.stream.entity.mapper.ReportHistory;
import com.umc.bscene.domain.stream.entity.mapper.StreamMember;
import com.umc.bscene.domain.stream.enums.ReportType;
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
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * StreamServiceImpl의 라이브 예약 편집/취소, 라이브 멤버 조회, 유저 신고 동작 단위 테스트.
 * 성능 튜닝 시 회귀를 잡을 수 있도록 결과값뿐 아니라 "호출 형태"(호출 횟수/순서/인자)까지 고정한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StreamServiceImpl 예약 편집/취소 및 신고")
class StreamServiceImplReservationTest {

    private static final Long LIVE_ID = 1L;
    private static final Long BROADCASTER_ID = 100L;
    private static final Long BAND_ID = 7L;

    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private AudioStreamRepository audioStreamRepository;
    @Mock
    private StreamMemberRepository streamMemberRepository;
    @Mock
    private LiveAlarmRepository liveAlarmRepository;
    @Mock
    private StreamReplayRepository streamReplayRepository;
    @Mock
    private ReportHistoryRepository reportHistoryRepository;
    @Mock
    private UserPort userPort;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private BandMemberPort bandMemberPort;
    @Mock
    private FollowPort followPort;
    @Mock
    private NotifyPort notifyPort;
    @Mock
    private RestClient mtxRestClient;
    @Mock
    private ViewerSsePresence viewerSsePresence;
    @Mock
    private LiveChatRoomCloser liveChatRoomCloser;
    @Mock
    private DiscordMessageSender discordMessageSender;
    @Mock
    private ZSetOperations<String, String> zSetOperations;

    private StreamServiceImpl streamService;

    private User broadcaster;

    @BeforeEach
    void setUp() {
        streamService = new StreamServiceImpl(
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
                "https://hls.test",
                "https://webrtc.test",
                "mixer-secret"
        );

        broadcaster = StreamFixtures.bandUser(BROADCASTER_ID);
    }

    @AfterEach
    void tearDown() {
        TxSyncSupport.end();
    }

    // ---------------------------------------------------------------- helpers

    private static void assertStreamError(ThrowingCallable callable, StreamErrorCode expected) {
        assertThatThrownBy(callable)
                .isInstanceOf(StreamException.class)
                .extracting(thrown -> ((StreamException) thrown).getBaseResponseCode())
                .isEqualTo(expected);
    }

    private static AudioStream scheduledStream() {
        return StreamFixtures.scheduledStream(LIVE_ID, BROADCASTER_ID, BAND_ID, LocalDateTime.of(2030, 1, 1, 21, 0));
    }

    private static ArgumentCaptor<List<StreamMember>> memberListCaptor() {
        return ArgumentCaptor.captor();
    }

    private static List<Long> userIdsOf(List<StreamMember> members) {
        return members.stream().map(sm -> sm.getUser().getId()).toList();
    }

    // ------------------------------------------------------- getReservationForEdit

    @Nested
    @DisplayName("getReservationForEdit - 예약 편집 화면 조회")
    class GetReservationForEdit {

        @Test
        @DisplayName("라이브가 없으면 AUDIO_STREAM_NOT_FOUND")
        void throwsWhenStreamNotFound() {
            when(audioStreamRepository.findById(LIVE_ID)).thenReturn(Optional.empty());

            assertStreamError(
                    () -> streamService.getReservationForEdit(broadcaster, LIVE_ID),
                    StreamErrorCode.AUDIO_STREAM_NOT_FOUND
            );
        }

        @Test
        @DisplayName("팬 모드 요청이면 FORBIDDEN_REQUEST")
        void throwsWhenFanMode() {
            User fan = StreamFixtures.fanUser(200L);
            when(audioStreamRepository.findById(LIVE_ID)).thenReturn(Optional.of(scheduledStream()));

            assertStreamError(
                    () -> streamService.getReservationForEdit(fan, LIVE_ID),
                    StreamErrorCode.FORBIDDEN_REQUEST
            );

            // 팬 모드는 밴드 소속 검사에 도달하지 않는다
            verify(bandMemberPort, never()).isActiveRegularMemberOfBand(anyLong(), anyLong());
        }

        @Test
        @DisplayName("활성 밴드가 라이브 생성 밴드와 다르면 FORBIDDEN_REQUEST")
        void throwsWhenBandMismatch() {
            when(audioStreamRepository.findById(LIVE_ID)).thenReturn(Optional.of(scheduledStream()));
            when(bandMemberPort.isActiveRegularMemberOfBand(BAND_ID, BROADCASTER_ID)).thenReturn(false);

            assertStreamError(
                    () -> streamService.getReservationForEdit(broadcaster, LIVE_ID),
                    StreamErrorCode.FORBIDDEN_REQUEST
            );
        }

        @Test
        @DisplayName("SCHEDULED가 아니면 AUDIO_STREAM_NOT_SCHEDULED")
        void throwsWhenNotScheduled() {
            AudioStream open = StreamFixtures.stream(LIVE_ID, BROADCASTER_ID, BAND_ID, StreamStatus.OPEN);
            when(audioStreamRepository.findById(LIVE_ID)).thenReturn(Optional.of(open));
            when(bandMemberPort.isActiveRegularMemberOfBand(BAND_ID, BROADCASTER_ID)).thenReturn(true);

            assertStreamError(
                    () -> streamService.getReservationForEdit(broadcaster, LIVE_ID),
                    StreamErrorCode.AUDIO_STREAM_NOT_SCHEDULED
            );
        }

        @Test
        @DisplayName("권한(403) 검사가 상태(409) 검사보다 먼저라, 권한 없는 유저는 예약 상태를 열거할 수 없다")
        void permissionIsCheckedBeforeState() {
            // 권한도 없고 상태도 SCHEDULED가 아닌 상황: 상태 노출 없이 403이어야 한다
            AudioStream closed = StreamFixtures.stream(LIVE_ID, BROADCASTER_ID, BAND_ID, StreamStatus.CLOSED);
            User outsider = StreamFixtures.bandUser(999L);
            when(audioStreamRepository.findById(LIVE_ID)).thenReturn(Optional.of(closed));
            when(bandMemberPort.isActiveRegularMemberOfBand(BAND_ID, 999L)).thenReturn(false);

            assertStreamError(
                    () -> streamService.getReservationForEdit(outsider, LIVE_ID),
                    StreamErrorCode.FORBIDDEN_REQUEST
            );

            // 상태 검사에 도달했다면 멤버 조회까지 갔을 것
            verify(streamMemberRepository, never()).findAllByAudioStream_Id(anyLong());
        }

        @Test
        @DisplayName("후보 수와 무관하게 멤버 조회와 후보 조회는 각각 1회만 수행한다 (N+1 방지)")
        void queriesEachSourceExactlyOnce() {
            AudioStream stream = scheduledStream();
            when(audioStreamRepository.findById(LIVE_ID)).thenReturn(Optional.of(stream));
            when(bandMemberPort.isActiveRegularMemberOfBand(BAND_ID, BROADCASTER_ID)).thenReturn(true);
            when(streamMemberRepository.findAllByAudioStream_Id(LIVE_ID)).thenReturn(List.of(
                    StreamFixtures.member(1L, broadcaster, stream, StreamMemberStatus.ACCEPTED),
                    StreamFixtures.member(2L, StreamFixtures.bandUser(101L), stream, StreamMemberStatus.INVITED),
                    StreamFixtures.member(3L, StreamFixtures.bandUser(102L), stream, StreamMemberStatus.ACCEPTED),
                    StreamFixtures.member(4L, StreamFixtures.bandUser(103L), stream, StreamMemberStatus.REJECTED)
            ));
            when(bandMemberPort.getCoHostCandidatesByBandId(BAND_ID)).thenReturn(List.of(
                    StreamFixtures.candidate(BROADCASTER_ID, 10L),
                    StreamFixtures.candidate(101L, 11L),
                    StreamFixtures.candidate(102L, 12L),
                    StreamFixtures.candidate(103L, 13L),
                    StreamFixtures.candidate(104L, 14L),
                    StreamFixtures.candidate(105L, 15L)
            ));

            ReservationEditResponse response = streamService.getReservationForEdit(broadcaster, LIVE_ID);

            assertThat(response.coHostCandidates()).hasSize(6);
            verify(streamMemberRepository, times(1)).findAllByAudioStream_Id(LIVE_ID);
            verify(bandMemberPort, times(1)).getCoHostCandidatesByBandId(BAND_ID);
        }

        @Test
        @DisplayName("후보는 송출자의 현재 밴드가 아니라 라이브 생성 시 확정된 밴드 기준으로 조회한다")
        void looksUpCandidatesByStreamBandId() {
            AudioStream stream = scheduledStream();
            when(audioStreamRepository.findById(LIVE_ID)).thenReturn(Optional.of(stream));
            when(bandMemberPort.isActiveRegularMemberOfBand(BAND_ID, BROADCASTER_ID)).thenReturn(true);
            when(streamMemberRepository.findAllByAudioStream_Id(LIVE_ID)).thenReturn(List.of());
            when(bandMemberPort.getCoHostCandidatesByBandId(BAND_ID)).thenReturn(List.of());

            streamService.getReservationForEdit(broadcaster, LIVE_ID);

            verify(bandMemberPort).getCoHostCandidatesByBandId(BAND_ID);
            verify(bandMemberPort, never()).getBandSummaryByBroadcasterId(anyLong());
        }

        @Test
        @DisplayName("응답 필드는 라이브 스냅샷을 그대로 pre-fill 한다")
        void mapsStreamFieldsForPrefill() {
            AudioStream stream = scheduledStream();
            when(audioStreamRepository.findById(LIVE_ID)).thenReturn(Optional.of(stream));
            when(bandMemberPort.isActiveRegularMemberOfBand(BAND_ID, BROADCASTER_ID)).thenReturn(true);
            when(streamMemberRepository.findAllByAudioStream_Id(LIVE_ID)).thenReturn(List.of());
            when(bandMemberPort.getCoHostCandidatesByBandId(BAND_ID)).thenReturn(List.of());

            ReservationEditResponse response = streamService.getReservationForEdit(broadcaster, LIVE_ID);

            assertThat(response.liveId()).isEqualTo(LIVE_ID);
            assertThat(response.title()).isEqualTo(stream.getTitle());
            assertThat(response.description()).isEqualTo(stream.getDescription());
            assertThat(response.thumbnailImageUrl()).isEqualTo(stream.getThumbnailImageUrl());
            assertThat(response.scheduledAt()).isEqualTo(stream.getScheduledAt());
            assertThat(response.coHostCandidates()).isEmpty();
        }

        @Test
        @DisplayName("상태 매핑: 송출자는 OWNER, ACCEPTED는 APPROVED, INVITED/REJECTED는 그대로, 행이 없으면 null")
        void mapsCandidateStatuses() {
            AudioStream stream = scheduledStream();
            when(audioStreamRepository.findById(LIVE_ID)).thenReturn(Optional.of(stream));
            when(bandMemberPort.isActiveRegularMemberOfBand(BAND_ID, BROADCASTER_ID)).thenReturn(true);
            when(streamMemberRepository.findAllByAudioStream_Id(LIVE_ID)).thenReturn(List.of(
                    StreamFixtures.member(1L, broadcaster, stream, StreamMemberStatus.ACCEPTED),
                    StreamFixtures.member(2L, StreamFixtures.bandUser(101L), stream, StreamMemberStatus.ACCEPTED),
                    StreamFixtures.member(3L, StreamFixtures.bandUser(102L), stream, StreamMemberStatus.INVITED),
                    StreamFixtures.member(4L, StreamFixtures.bandUser(103L), stream, StreamMemberStatus.REJECTED)
            ));
            when(bandMemberPort.getCoHostCandidatesByBandId(BAND_ID)).thenReturn(List.of(
                    StreamFixtures.candidate(BROADCASTER_ID, 1L),
                    StreamFixtures.candidate(101L, 2L),
                    StreamFixtures.candidate(102L, 3L),
                    StreamFixtures.candidate(104L, 4L),
                    StreamFixtures.candidate(103L, 5L)
            ));

            ReservationEditResponse response = streamService.getReservationForEdit(broadcaster, LIVE_ID);

            assertThat(response.coHostCandidates())
                    .extracting(CoHostCandidateResponse::bandMemberId)
                    .containsExactly(1L, 2L, 3L, 4L, 5L);
            assertThat(response.coHostCandidates())
                    .extracting(CoHostCandidateResponse::status)
                    .containsExactly(
                            StreamMemberStatus.OWNER,
                            StreamMemberStatus.APPROVED,
                            StreamMemberStatus.INVITED,
                            null,
                            StreamMemberStatus.REJECTED
                    );
        }

        @Test
        @DisplayName("정렬 순서: OWNER → APPROVED → INVITED → 미선택(null) → REJECTED, 동순위는 bandMemberId 오름차순")
        void sortsCandidatesByStatusRankThenBandMemberId() {
            AudioStream stream = scheduledStream();
            when(audioStreamRepository.findById(LIVE_ID)).thenReturn(Optional.of(stream));
            when(bandMemberPort.isActiveRegularMemberOfBand(BAND_ID, BROADCASTER_ID)).thenReturn(true);
            when(streamMemberRepository.findAllByAudioStream_Id(LIVE_ID)).thenReturn(List.of(
                    StreamFixtures.member(1L, broadcaster, stream, StreamMemberStatus.ACCEPTED),
                    StreamFixtures.member(2L, StreamFixtures.bandUser(101L), stream, StreamMemberStatus.ACCEPTED),
                    StreamFixtures.member(3L, StreamFixtures.bandUser(102L), stream, StreamMemberStatus.ACCEPTED),
                    StreamFixtures.member(4L, StreamFixtures.bandUser(103L), stream, StreamMemberStatus.INVITED),
                    StreamFixtures.member(5L, StreamFixtures.bandUser(104L), stream, StreamMemberStatus.INVITED),
                    StreamFixtures.member(6L, StreamFixtures.bandUser(107L), stream, StreamMemberStatus.REJECTED),
                    StreamFixtures.member(7L, StreamFixtures.bandUser(108L), stream, StreamMemberStatus.REJECTED)
            ));
            // 입력 순서를 일부러 뒤섞어 정렬이 실제로 수행되는지 확인
            when(bandMemberPort.getCoHostCandidatesByBandId(BAND_ID)).thenReturn(List.of(
                    StreamFixtures.candidate(108L, 25L),     // REJECTED
                    StreamFixtures.candidate(101L, 30L),     // APPROVED
                    StreamFixtures.candidate(105L, 40L),     // 미선택
                    StreamFixtures.candidate(104L, 5L),      // INVITED
                    StreamFixtures.candidate(BROADCASTER_ID, 50L),  // OWNER
                    StreamFixtures.candidate(107L, 60L),     // REJECTED
                    StreamFixtures.candidate(103L, 20L),     // INVITED
                    StreamFixtures.candidate(106L, 15L),     // 미선택
                    StreamFixtures.candidate(102L, 10L)      // APPROVED
            ));

            ReservationEditResponse response = streamService.getReservationForEdit(broadcaster, LIVE_ID);

            assertThat(response.coHostCandidates())
                    .extracting(CoHostCandidateResponse::bandMemberId)
                    .containsExactly(50L, 10L, 30L, 5L, 20L, 15L, 40L, 25L, 60L);
            assertThat(response.coHostCandidates())
                    .extracting(CoHostCandidateResponse::status)
                    .containsExactly(
                            StreamMemberStatus.OWNER,
                            StreamMemberStatus.APPROVED,
                            StreamMemberStatus.APPROVED,
                            StreamMemberStatus.INVITED,
                            StreamMemberStatus.INVITED,
                            null,
                            null,
                            StreamMemberStatus.REJECTED,
                            StreamMemberStatus.REJECTED
                    );
        }

        @Test
        @DisplayName("같은 userId의 StreamMember 행이 중복돼도 예외 없이 첫 행 상태로 병합된다")
        void collapsesDuplicateMemberRows() {
            AudioStream stream = scheduledStream();
            User duplicated = StreamFixtures.bandUser(101L);
            when(audioStreamRepository.findById(LIVE_ID)).thenReturn(Optional.of(stream));
            when(bandMemberPort.isActiveRegularMemberOfBand(BAND_ID, BROADCASTER_ID)).thenReturn(true);
            when(streamMemberRepository.findAllByAudioStream_Id(LIVE_ID)).thenReturn(List.of(
                    StreamFixtures.member(1L, duplicated, stream, StreamMemberStatus.ACCEPTED),
                    StreamFixtures.member(2L, StreamFixtures.bandUser(101L), stream, StreamMemberStatus.REJECTED)
            ));
            when(bandMemberPort.getCoHostCandidatesByBandId(BAND_ID)).thenReturn(List.of(
                    StreamFixtures.candidate(101L, 2L)
            ));

            ReservationEditResponse response = streamService.getReservationForEdit(broadcaster, LIVE_ID);

            assertThat(response.coHostCandidates())
                    .extracting(CoHostCandidateResponse::status)
                    .containsExactly(StreamMemberStatus.APPROVED);
        }
    }

    // ---------------------------------------------------------- updateReservation

    @Nested
    @DisplayName("updateReservation - 예약 수정")
    class UpdateReservation {

        private ReservationPatchRequest patch(String title, List<Long> coHost) {
            return new ReservationPatchRequest(title, null, null, null, coHost);
        }

        @Test
        @DisplayName("라이브가 없으면 AUDIO_STREAM_NOT_FOUND")
        void throwsWhenStreamNotFound() {
            when(audioStreamRepository.findByIdForUpdate(LIVE_ID)).thenReturn(Optional.empty());

            assertStreamError(
                    () -> streamService.updateReservation(broadcaster, LIVE_ID, patch("t", null)),
                    StreamErrorCode.AUDIO_STREAM_NOT_FOUND
            );
        }

        @Test
        @DisplayName("findById가 아니라 X-lock 조회(findByIdForUpdate)를 사용한다")
        void usesPessimisticLockLookup() {
            when(audioStreamRepository.findByIdForUpdate(LIVE_ID)).thenReturn(Optional.of(scheduledStream()));
            when(bandMemberPort.isActiveRegularMemberOfBand(BAND_ID, BROADCASTER_ID)).thenReturn(true);
            when(audioStreamRepository.updateReservationIfScheduled(anyLong(), any(), any(), any(), any()))
                    .thenReturn(1);

            streamService.updateReservation(broadcaster, LIVE_ID, patch("새 제목", null));

            verify(audioStreamRepository).findByIdForUpdate(LIVE_ID);
            verify(audioStreamRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("팬 모드 요청이면 FORBIDDEN_REQUEST")
        void throwsWhenFanMode() {
            User fan = StreamFixtures.fanUser(200L);
            when(audioStreamRepository.findByIdForUpdate(LIVE_ID)).thenReturn(Optional.of(scheduledStream()));

            assertStreamError(
                    () -> streamService.updateReservation(fan, LIVE_ID, patch("t", null)),
                    StreamErrorCode.FORBIDDEN_REQUEST
            );

            verify(audioStreamRepository, never())
                    .updateReservationIfScheduled(anyLong(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("활성 밴드가 라이브 생성 밴드와 다르면 FORBIDDEN_REQUEST")
        void throwsWhenBandMismatch() {
            when(audioStreamRepository.findByIdForUpdate(LIVE_ID)).thenReturn(Optional.of(scheduledStream()));
            when(bandMemberPort.isActiveRegularMemberOfBand(BAND_ID, BROADCASTER_ID)).thenReturn(false);

            assertStreamError(
                    () -> streamService.updateReservation(broadcaster, LIVE_ID, patch("t", null)),
                    StreamErrorCode.FORBIDDEN_REQUEST
            );
        }

        @Test
        @DisplayName("SCHEDULED가 아니면 AUDIO_STREAM_NOT_SCHEDULED")
        void throwsWhenNotScheduled() {
            AudioStream open = StreamFixtures.stream(LIVE_ID, BROADCASTER_ID, BAND_ID, StreamStatus.OPEN);
            when(audioStreamRepository.findByIdForUpdate(LIVE_ID)).thenReturn(Optional.of(open));
            when(bandMemberPort.isActiveRegularMemberOfBand(BAND_ID, BROADCASTER_ID)).thenReturn(true);

            assertStreamError(
                    () -> streamService.updateReservation(broadcaster, LIVE_ID, patch("t", null)),
                    StreamErrorCode.AUDIO_STREAM_NOT_SCHEDULED
            );

            verify(audioStreamRepository, never())
                    .updateReservationIfScheduled(anyLong(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("조건부 UPDATE가 0행이면 AUDIO_STREAM_NOT_SCHEDULED (조회-갱신 사이 상태 전이)")
        void throwsWhenConditionalUpdateAffectsNoRow() {
            when(audioStreamRepository.findByIdForUpdate(LIVE_ID)).thenReturn(Optional.of(scheduledStream()));
            when(bandMemberPort.isActiveRegularMemberOfBand(BAND_ID, BROADCASTER_ID)).thenReturn(true);
            when(audioStreamRepository.updateReservationIfScheduled(anyLong(), any(), any(), any(), any()))
                    .thenReturn(0);

            assertStreamError(
                    () -> streamService.updateReservation(broadcaster, LIVE_ID, patch("t", List.of(101L))),
                    StreamErrorCode.AUDIO_STREAM_NOT_SCHEDULED
            );

            // 갱신 실패 시 공동 진행 목록은 손대지 않는다
            verify(streamMemberRepository, never()).findAllByAudioStream_Id(anyLong());
        }

        @Test
        @DisplayName("PATCH 시맨틱: null 필드는 null 그대로 전달되어 쿼리의 coalesce가 기존 값을 유지하게 한다")
        void forwardsNullFieldsAsNull() {
            LocalDateTime newScheduledAt = LocalDateTime.of(2031, 3, 4, 20, 30);
            when(audioStreamRepository.findByIdForUpdate(LIVE_ID)).thenReturn(Optional.of(scheduledStream()));
            when(bandMemberPort.isActiveRegularMemberOfBand(BAND_ID, BROADCASTER_ID)).thenReturn(true);
            when(audioStreamRepository.updateReservationIfScheduled(anyLong(), any(), any(), any(), any()))
                    .thenReturn(1);

            streamService.updateReservation(
                    broadcaster, LIVE_ID,
                    new ReservationPatchRequest("바뀐 제목", null, null, newScheduledAt, null)
            );

            verify(audioStreamRepository)
                    .updateReservationIfScheduled(LIVE_ID, "바뀐 제목", null, null, newScheduledAt);
        }

        @Test
        @DisplayName("coHost가 null이면 공동 진행 교체 로직 자체를 수행하지 않는다")
        void skipsCoHostReplacementWhenCoHostIsNull() {
            when(audioStreamRepository.findByIdForUpdate(LIVE_ID)).thenReturn(Optional.of(scheduledStream()));
            when(bandMemberPort.isActiveRegularMemberOfBand(BAND_ID, BROADCASTER_ID)).thenReturn(true);
            when(audioStreamRepository.updateReservationIfScheduled(anyLong(), any(), any(), any(), any()))
                    .thenReturn(1);

            streamService.updateReservation(broadcaster, LIVE_ID, patch("새 제목", null));

            verify(streamMemberRepository, never()).findAllByAudioStream_Id(anyLong());
            verify(streamMemberRepository, never()).deleteAll(anyList());
            verify(streamMemberRepository, never()).saveAll(anyList());
            verify(streamMemberRepository, never()).flush();
            verify(bandMemberPort, never()).getCoHostCandidatesByBandId(anyLong());
        }

        @Test
        @DisplayName("coHost가 빈 리스트면 기존 공동 진행자를 모두 삭제하고 후보 조회는 하지 않는다")
        void clearsAllCoHostsWhenEmptyList() {
            AudioStream stream = scheduledStream();
            when(audioStreamRepository.findByIdForUpdate(LIVE_ID)).thenReturn(Optional.of(stream));
            when(bandMemberPort.isActiveRegularMemberOfBand(BAND_ID, BROADCASTER_ID)).thenReturn(true);
            when(audioStreamRepository.updateReservationIfScheduled(anyLong(), any(), any(), any(), any()))
                    .thenReturn(1);
            when(streamMemberRepository.findAllByAudioStream_Id(LIVE_ID)).thenReturn(List.of(
                    StreamFixtures.member(1L, broadcaster, stream, StreamMemberStatus.ACCEPTED),
                    StreamFixtures.member(2L, StreamFixtures.bandUser(101L), stream, StreamMemberStatus.INVITED),
                    StreamFixtures.member(3L, StreamFixtures.bandUser(102L), stream, StreamMemberStatus.ACCEPTED)
            ));

            streamService.updateReservation(broadcaster, LIVE_ID, patch(null, List.of()));

            ArgumentCaptor<List<StreamMember>> deleted = memberListCaptor();
            verify(streamMemberRepository).deleteAll(deleted.capture());
            assertThat(userIdsOf(deleted.getValue())).containsExactly(101L, 102L);

            verify(streamMemberRepository, never()).saveAll(anyList());
            verify(streamMemberRepository, never()).flush();
            verify(bandMemberPort, never()).getCoHostCandidatesByBandId(anyLong());
            verify(userPort, never()).findAllByIds(anyCollection());
        }
    }

    // ------------------------------------------------------------ replaceCoHosts

    @Nested
    @DisplayName("replaceCoHosts - 공동 진행자 교체 (updateReservation 경유)")
    class ReplaceCoHosts {

        private AudioStream stream;

        @BeforeEach
        void givenEditableScheduledStream() {
            stream = scheduledStream();
            when(audioStreamRepository.findByIdForUpdate(LIVE_ID)).thenReturn(Optional.of(stream));
            when(bandMemberPort.isActiveRegularMemberOfBand(BAND_ID, BROADCASTER_ID)).thenReturn(true);
            when(audioStreamRepository.updateReservationIfScheduled(anyLong(), any(), any(), any(), any()))
                    .thenReturn(1);
        }

        private void givenCurrentRows(StreamMember... rows) {
            when(streamMemberRepository.findAllByAudioStream_Id(LIVE_ID)).thenReturn(List.of(rows));
        }

        private void givenCandidates(Long... userIds) {
            List<CoHostCandidateInfo> candidates = new java.util.ArrayList<>();
            long bandMemberId = 1L;
            for (Long userId : userIds)
                candidates.add(StreamFixtures.candidate(userId, bandMemberId++));

            when(bandMemberPort.getCoHostCandidatesByBandId(BAND_ID)).thenReturn(candidates);
        }

        private void update(List<Long> coHost) {
            streamService.updateReservation(
                    broadcaster, LIVE_ID, new ReservationPatchRequest(null, null, null, null, coHost)
            );
        }

        @Test
        @DisplayName("송출자 본인 행은 공동 진행자로 취급되지 않아 삭제 대상에 포함되지 않는다")
        void neverTreatsBroadcasterRowAsCoHost() {
            givenCurrentRows(
                    StreamFixtures.member(1L, broadcaster, stream, StreamMemberStatus.ACCEPTED),
                    StreamFixtures.member(2L, StreamFixtures.bandUser(101L), stream, StreamMemberStatus.INVITED)
            );

            update(List.of());

            ArgumentCaptor<List<StreamMember>> deleted = memberListCaptor();
            verify(streamMemberRepository).deleteAll(deleted.capture());
            assertThat(userIdsOf(deleted.getValue()))
                    .containsExactly(101L)
                    .doesNotContain(BROADCASTER_ID);
        }

        @Test
        @DisplayName("후보가 아닌 유저를 추가하면 INVALID_CO_HOST")
        void throwsWhenAddingNonCandidate() {
            givenCurrentRows();
            givenCandidates(BROADCASTER_ID, 101L);

            assertStreamError(() -> update(List.of(999L)), StreamErrorCode.INVALID_CO_HOST);

            // 후보 검증 실패 시 삭제/삽입까지 진행하지 않는다
            verify(streamMemberRepository, never()).deleteAll(anyList());
            verify(streamMemberRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("송출자 본인을 공동 진행자로 지정하면 후보 목록에 있어도 INVALID_CO_HOST")
        void throwsWhenBroadcasterAddsSelfAsCoHost() {
            givenCurrentRows(StreamFixtures.member(1L, broadcaster, stream, StreamMemberStatus.ACCEPTED));
            givenCandidates(BROADCASTER_ID, 101L);

            assertStreamError(() -> update(List.of(BROADCASTER_ID)), StreamErrorCode.INVALID_CO_HOST);
        }

        @Test
        @DisplayName("유지되는 INVITED/ACCEPTED 멤버는 삭제·재생성되지 않아 수락 상태가 리셋되지 않는다")
        void keepsExistingMembersWithoutRecreating() {
            givenCurrentRows(
                    StreamFixtures.member(1L, broadcaster, stream, StreamMemberStatus.ACCEPTED),
                    StreamFixtures.member(2L, StreamFixtures.bandUser(101L), stream, StreamMemberStatus.ACCEPTED),
                    StreamFixtures.member(3L, StreamFixtures.bandUser(102L), stream, StreamMemberStatus.INVITED)
            );
            givenCandidates(BROADCASTER_ID, 101L, 102L, 103L);
            when(userPort.findAllByIds(anyCollection()))
                    .thenReturn(List.of(StreamFixtures.bandUser(103L)));

            update(List.of(101L, 102L, 103L));

            ArgumentCaptor<List<StreamMember>> deleted = memberListCaptor();
            verify(streamMemberRepository).deleteAll(deleted.capture());
            assertThat(deleted.getValue()).isEmpty();

            ArgumentCaptor<List<StreamMember>> saved = memberListCaptor();
            verify(streamMemberRepository).saveAll(saved.capture());
            assertThat(userIdsOf(saved.getValue())).containsExactly(103L);

            // 신규 대상만 실조회한다
            verify(userPort).findAllByIds(Set.of(103L));
        }

        @Test
        @DisplayName("밴드를 떠난 기존 공동 진행자를 그대로 재제출하면 후보 검증 없이 유지된다")
        void retainsExistingCoHostWhoLeftBandWithoutCandidateLookup() {
            givenCurrentRows(
                    StreamFixtures.member(1L, broadcaster, stream, StreamMemberStatus.ACCEPTED),
                    StreamFixtures.member(2L, StreamFixtures.bandUser(101L), stream, StreamMemberStatus.INVITED)
            );

            update(List.of(101L));

            ArgumentCaptor<List<StreamMember>> deleted = memberListCaptor();
            verify(streamMemberRepository).deleteAll(deleted.capture());
            assertThat(deleted.getValue()).isEmpty();

            verify(bandMemberPort, never()).getCoHostCandidatesByBandId(anyLong());
            verify(userPort, never()).findAllByIds(anyCollection());
            verify(streamMemberRepository, never()).saveAll(anyList());
            verify(streamMemberRepository, never()).flush();
        }

        @Test
        @DisplayName("REJECTED 멤버를 다시 포함하면 재초대: 기존 행 삭제 후 INVITED로 재생성")
        void reInvitesRejectedMember() {
            givenCurrentRows(
                    StreamFixtures.member(1L, broadcaster, stream, StreamMemberStatus.ACCEPTED),
                    StreamFixtures.member(2L, StreamFixtures.bandUser(101L), stream, StreamMemberStatus.REJECTED)
            );
            givenCandidates(BROADCASTER_ID, 101L);
            when(userPort.findAllByIds(anyCollection()))
                    .thenReturn(List.of(StreamFixtures.bandUser(101L)));

            update(List.of(101L));

            ArgumentCaptor<List<StreamMember>> deleted = memberListCaptor();
            verify(streamMemberRepository).deleteAll(deleted.capture());
            assertThat(userIdsOf(deleted.getValue())).containsExactly(101L);

            ArgumentCaptor<List<StreamMember>> saved = memberListCaptor();
            verify(streamMemberRepository).saveAll(saved.capture());
            assertThat(saved.getValue()).hasSize(1);
            assertThat(saved.getValue().get(0).getUser().getId()).isEqualTo(101L);
            assertThat(saved.getValue().get(0).getStatus()).isEqualTo(StreamMemberStatus.INVITED);
        }

        @Test
        @DisplayName("요청에서 빠진 REJECTED 멤버도 그대로 삭제된다")
        void deletesRejectedMemberNotInRequest() {
            givenCurrentRows(
                    StreamFixtures.member(1L, broadcaster, stream, StreamMemberStatus.ACCEPTED),
                    StreamFixtures.member(2L, StreamFixtures.bandUser(101L), stream, StreamMemberStatus.REJECTED),
                    StreamFixtures.member(3L, StreamFixtures.bandUser(102L), stream, StreamMemberStatus.INVITED)
            );

            update(List.of(102L));

            ArgumentCaptor<List<StreamMember>> deleted = memberListCaptor();
            verify(streamMemberRepository).deleteAll(deleted.capture());
            assertThat(userIdsOf(deleted.getValue())).containsExactly(101L);

            verify(streamMemberRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("삭제와 삽입이 함께 있으면 deleteAll → flush → saveAll → flush 순서를 지킨다 (unique 제약 회피)")
        void flushesDeletesBeforeInserts() {
            givenCurrentRows(
                    StreamFixtures.member(1L, broadcaster, stream, StreamMemberStatus.ACCEPTED),
                    StreamFixtures.member(2L, StreamFixtures.bandUser(101L), stream, StreamMemberStatus.REJECTED)
            );
            givenCandidates(BROADCASTER_ID, 101L);
            when(userPort.findAllByIds(anyCollection()))
                    .thenReturn(List.of(StreamFixtures.bandUser(101L)));

            update(List.of(101L));

            InOrder order = inOrder(streamMemberRepository);
            order.verify(streamMemberRepository).deleteAll(anyList());
            order.verify(streamMemberRepository).flush();
            order.verify(streamMemberRepository).saveAll(anyList());
            order.verify(streamMemberRepository).flush();

            // 삭제 반영용 1회 + 삽입 후 제약 검사용 1회
            verify(streamMemberRepository, times(2)).flush();
        }

        @Test
        @DisplayName("삭제 대상이 없으면 삽입 전 flush를 생략한다")
        void skipsPreInsertFlushWhenNothingDeleted() {
            givenCurrentRows(StreamFixtures.member(1L, broadcaster, stream, StreamMemberStatus.ACCEPTED));
            givenCandidates(BROADCASTER_ID, 101L);
            when(userPort.findAllByIds(anyCollection()))
                    .thenReturn(List.of(StreamFixtures.bandUser(101L)));

            update(List.of(101L));

            // 삽입 후 unique 제약 감지용 flush 1회만 수행
            verify(streamMemberRepository, times(1)).flush();

            InOrder order = inOrder(streamMemberRepository);
            order.verify(streamMemberRepository).deleteAll(anyList());
            order.verify(streamMemberRepository).saveAll(anyList());
            order.verify(streamMemberRepository).flush();
        }

        @Test
        @DisplayName("실조회 결과가 요청 수보다 적으면 INVALID_CO_HOST")
        void throwsWhenSomeUsersMissing() {
            givenCurrentRows();
            givenCandidates(BROADCASTER_ID, 101L, 102L);
            when(userPort.findAllByIds(anyCollection()))
                    .thenReturn(List.of(StreamFixtures.bandUser(101L)));

            assertStreamError(() -> update(List.of(101L, 102L)), StreamErrorCode.INVALID_CO_HOST);

            verify(streamMemberRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("삽입 중 unique 제약 위반이면 CO_HOST_CONFLICT")
        void throwsCoHostConflictOnDataIntegrityViolation() {
            givenCurrentRows();
            givenCandidates(BROADCASTER_ID, 101L);
            when(userPort.findAllByIds(anyCollection()))
                    .thenReturn(List.of(StreamFixtures.bandUser(101L)));
            doThrow(new DataIntegrityViolationException("uk_stream_member_user_stream"))
                    .when(streamMemberRepository).saveAll(anyList());

            assertStreamError(() -> update(List.of(101L)), StreamErrorCode.CO_HOST_CONFLICT);
        }

        @Test
        @DisplayName("새 공동 진행자 행은 모두 INVITED 상태로 생성된다")
        void createsNewRowsAsInvited() {
            givenCurrentRows();
            givenCandidates(BROADCASTER_ID, 101L, 102L);
            when(userPort.findAllByIds(anyCollection()))
                    .thenReturn(List.of(StreamFixtures.bandUser(101L), StreamFixtures.bandUser(102L)));

            update(List.of(101L, 102L));

            ArgumentCaptor<List<StreamMember>> saved = memberListCaptor();
            verify(streamMemberRepository).saveAll(saved.capture());
            assertThat(saved.getValue()).hasSize(2);
            assertThat(saved.getValue())
                    .allSatisfy(row -> {
                        assertThat(row.getStatus()).isEqualTo(StreamMemberStatus.INVITED);
                        assertThat(row.getAudioStream()).isSameAs(stream);
                    });
            assertThat(userIdsOf(saved.getValue())).containsExactlyInAnyOrder(101L, 102L);
        }

        @Test
        @DisplayName("요청에 같은 id가 중복돼도 한 행으로 합쳐진다")
        void collapsesDuplicateRequestedIds() {
            givenCurrentRows();
            givenCandidates(BROADCASTER_ID, 101L);
            when(userPort.findAllByIds(anyCollection()))
                    .thenReturn(List.of(StreamFixtures.bandUser(101L)));

            update(List.of(101L, 101L, 101L));

            verify(userPort).findAllByIds(Set.of(101L));

            ArgumentCaptor<List<StreamMember>> saved = memberListCaptor();
            verify(streamMemberRepository).saveAll(saved.capture());
            assertThat(userIdsOf(saved.getValue())).containsExactly(101L);
        }
    }

    // ---------------------------------------------------------- cancelReservation

    @Nested
    @DisplayName("cancelReservation - 예약 취소")
    class CancelReservation {

        @Test
        @DisplayName("라이브가 없으면 AUDIO_STREAM_NOT_FOUND")
        void throwsWhenStreamNotFound() {
            when(audioStreamRepository.findByIdForUpdate(LIVE_ID)).thenReturn(Optional.empty());

            assertStreamError(
                    () -> streamService.cancelReservation(broadcaster, LIVE_ID),
                    StreamErrorCode.AUDIO_STREAM_NOT_FOUND
            );
        }

        @Test
        @DisplayName("팬 모드 요청이면 FORBIDDEN_REQUEST")
        void throwsWhenFanMode() {
            User fan = StreamFixtures.fanUser(200L);
            when(audioStreamRepository.findByIdForUpdate(LIVE_ID)).thenReturn(Optional.of(scheduledStream()));

            assertStreamError(
                    () -> streamService.cancelReservation(fan, LIVE_ID),
                    StreamErrorCode.FORBIDDEN_REQUEST
            );
        }

        @Test
        @DisplayName("활성 밴드가 라이브 생성 밴드와 다르면 FORBIDDEN_REQUEST")
        void throwsWhenBandMismatch() {
            when(audioStreamRepository.findByIdForUpdate(LIVE_ID)).thenReturn(Optional.of(scheduledStream()));
            when(bandMemberPort.isActiveRegularMemberOfBand(BAND_ID, BROADCASTER_ID)).thenReturn(false);

            assertStreamError(
                    () -> streamService.cancelReservation(broadcaster, LIVE_ID),
                    StreamErrorCode.FORBIDDEN_REQUEST
            );
        }

        @Test
        @DisplayName("SCHEDULED가 아니면 AUDIO_STREAM_NOT_SCHEDULED")
        void throwsWhenNotScheduled() {
            AudioStream open = StreamFixtures.stream(LIVE_ID, BROADCASTER_ID, BAND_ID, StreamStatus.OPEN);
            when(audioStreamRepository.findByIdForUpdate(LIVE_ID)).thenReturn(Optional.of(open));
            when(bandMemberPort.isActiveRegularMemberOfBand(BAND_ID, BROADCASTER_ID)).thenReturn(true);

            assertStreamError(
                    () -> streamService.cancelReservation(broadcaster, LIVE_ID),
                    StreamErrorCode.AUDIO_STREAM_NOT_SCHEDULED
            );

            verify(audioStreamRepository, never()).cancelReservationIfScheduled(anyLong(), any());
        }

        @Test
        @DisplayName("조건부 취소 UPDATE가 0행이면 AUDIO_STREAM_NOT_SCHEDULED")
        void throwsWhenConditionalCancelAffectsNoRow() {
            when(audioStreamRepository.findByIdForUpdate(LIVE_ID)).thenReturn(Optional.of(scheduledStream()));
            when(bandMemberPort.isActiveRegularMemberOfBand(BAND_ID, BROADCASTER_ID)).thenReturn(true);
            when(audioStreamRepository.cancelReservationIfScheduled(anyLong(), any())).thenReturn(0);

            assertStreamError(
                    () -> streamService.cancelReservation(broadcaster, LIVE_ID),
                    StreamErrorCode.AUDIO_STREAM_NOT_SCHEDULED
            );

            verify(streamMemberRepository, never()).deleteAll(anyList());
        }

        @Test
        @DisplayName("X-lock 조회를 사용하며, 취소 성공 시 송출자 ACCEPTED 행까지 모든 진행자 행을 삭제한다")
        void deletesAllMemberRowsIncludingBroadcaster() {
            AudioStream stream = scheduledStream();
            when(audioStreamRepository.findByIdForUpdate(LIVE_ID)).thenReturn(Optional.of(stream));
            when(bandMemberPort.isActiveRegularMemberOfBand(BAND_ID, BROADCASTER_ID)).thenReturn(true);
            when(audioStreamRepository.cancelReservationIfScheduled(anyLong(), any())).thenReturn(1);
            when(streamMemberRepository.findAllByAudioStream_Id(LIVE_ID)).thenReturn(List.of(
                    StreamFixtures.member(1L, broadcaster, stream, StreamMemberStatus.ACCEPTED),
                    StreamFixtures.member(2L, StreamFixtures.bandUser(101L), stream, StreamMemberStatus.INVITED),
                    StreamFixtures.member(3L, StreamFixtures.bandUser(102L), stream, StreamMemberStatus.REJECTED)
            ));

            streamService.cancelReservation(broadcaster, LIVE_ID);

            verify(audioStreamRepository).findByIdForUpdate(LIVE_ID);
            verify(audioStreamRepository, never()).findById(anyLong());

            ArgumentCaptor<List<StreamMember>> deleted = memberListCaptor();
            verify(streamMemberRepository).deleteAll(deleted.capture());
            assertThat(userIdsOf(deleted.getValue())).containsExactly(BROADCASTER_ID, 101L, 102L);
        }
    }

    // ------------------------------------------------------------- getLiveMembers

    @Nested
    @DisplayName("getLiveMembers - 라이브 멤버 조회")
    class GetLiveMembers {

        @Test
        @DisplayName("라이브가 없으면 AUDIO_STREAM_NOT_FOUND")
        void throwsWhenStreamNotFound() {
            when(audioStreamRepository.findById(LIVE_ID)).thenReturn(Optional.empty());

            assertStreamError(
                    () -> streamService.getLiveMembers(LIVE_ID),
                    StreamErrorCode.AUDIO_STREAM_NOT_FOUND
            );
        }

        private void givenPresence(String... userIds) {
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.range("live-member:" + LIVE_ID, 0, -1))
                    .thenReturn(java.util.Set.of(userIds));
        }

        @Test
        @DisplayName("입장 중인 ACCEPTED 멤버만 조회하고, 라이브 생성 밴드 기준으로 프로필을 조회한다")
        void queriesAcceptedMembersAndResolvesProfilesByStreamBandId() {
            AudioStream stream = StreamFixtures.stream(LIVE_ID, BROADCASTER_ID, BAND_ID, StreamStatus.OPEN);
            LiveMembersResponse.LiveMemberProfileResponse profile =
                    new LiveMembersResponse.LiveMemberProfileResponse(
                            "https://cdn.test/band.jpg", "닉네임", "밴드이름", List.of(), true
                    );

            when(audioStreamRepository.findById(LIVE_ID)).thenReturn(Optional.of(stream));
            givenPresence(String.valueOf(BROADCASTER_ID), "101");
            when(streamMemberRepository.findAllByAudioStream_IdAndStatus(LIVE_ID, StreamMemberStatus.ACCEPTED))
                    .thenReturn(List.of(
                            StreamFixtures.member(1L, broadcaster, stream, StreamMemberStatus.ACCEPTED),
                            StreamFixtures.member(2L, StreamFixtures.bandUser(101L), stream, StreamMemberStatus.ACCEPTED)
                    ));
            when(bandMemberPort.getLiveMemberProfiles(BAND_ID, List.of(BROADCASTER_ID, 101L)))
                    .thenReturn(List.of(profile));

            LiveMembersResponse response = streamService.getLiveMembers(LIVE_ID);

            assertThat(response.members()).containsExactly(profile);
            verify(streamMemberRepository).findAllByAudioStream_IdAndStatus(LIVE_ID, StreamMemberStatus.ACCEPTED);
            verify(bandMemberPort).getLiveMemberProfiles(BAND_ID, List.of(BROADCASTER_ID, 101L));
        }

        @Test
        @DisplayName("ACCEPTED여도 퇴장했거나 입장한 적 없는(프레젠스에 없는) 멤버는 응답에서 제외된다")
        void excludesAcceptedMembersWhoAreNotPresent() {
            AudioStream stream = StreamFixtures.stream(LIVE_ID, BROADCASTER_ID, BAND_ID, StreamStatus.OPEN);
            when(audioStreamRepository.findById(LIVE_ID)).thenReturn(Optional.of(stream));

            // 101은 leaveRoom으로 퇴장, 102는 수락만 하고 미입장 → 송출자만 프레젠스에 존재
            givenPresence(String.valueOf(BROADCASTER_ID));
            when(streamMemberRepository.findAllByAudioStream_IdAndStatus(LIVE_ID, StreamMemberStatus.ACCEPTED))
                    .thenReturn(List.of(
                            StreamFixtures.member(1L, broadcaster, stream, StreamMemberStatus.ACCEPTED),
                            StreamFixtures.member(2L, StreamFixtures.bandUser(101L), stream, StreamMemberStatus.ACCEPTED),
                            StreamFixtures.member(3L, StreamFixtures.bandUser(102L), stream, StreamMemberStatus.ACCEPTED)
                    ));
            when(bandMemberPort.getLiveMemberProfiles(BAND_ID, List.of(BROADCASTER_ID))).thenReturn(List.of());

            streamService.getLiveMembers(LIVE_ID);

            verify(bandMemberPort).getLiveMemberProfiles(BAND_ID, List.of(BROADCASTER_ID));
        }

        @Test
        @DisplayName("확정 멤버가 없어도 빈 리스트로 프로필 포트를 1회 호출한다")
        void callsProfilePortOnceEvenWithNoMembers() {
            AudioStream stream = StreamFixtures.stream(LIVE_ID, BROADCASTER_ID, BAND_ID, StreamStatus.OPEN);
            when(audioStreamRepository.findById(LIVE_ID)).thenReturn(Optional.of(stream));
            givenPresence();
            when(streamMemberRepository.findAllByAudioStream_IdAndStatus(LIVE_ID, StreamMemberStatus.ACCEPTED))
                    .thenReturn(List.of());
            when(bandMemberPort.getLiveMemberProfiles(BAND_ID, List.of())).thenReturn(List.of());

            LiveMembersResponse response = streamService.getLiveMembers(LIVE_ID);

            assertThat(response.members()).isEmpty();
            verify(bandMemberPort, times(1)).getLiveMemberProfiles(BAND_ID, List.of());
        }
    }

    // ----------------------------------------------------------------- reportUser

    @Nested
    @DisplayName("reportUser - 라이브 내 유저 신고")
    class ReportUser {

        private static final Long TARGET_ID = 300L;

        private ReportUserRequest request(Long targetUserId) {
            return new ReportUserRequest(targetUserId, ReportType.ABUSE, "문제 채팅", "상세 코멘트");
        }

        @Test
        @DisplayName("라이브가 없으면 AUDIO_STREAM_NOT_FOUND")
        void throwsWhenStreamNotFound() {
            when(audioStreamRepository.findById(LIVE_ID)).thenReturn(Optional.empty());

            assertStreamError(
                    () -> streamService.reportUser(broadcaster, LIVE_ID, request(TARGET_ID)),
                    StreamErrorCode.AUDIO_STREAM_NOT_FOUND
            );
        }

        @Test
        @DisplayName("자기 자신을 신고하면 SELF_REPORT_NOT_ALLOWED")
        void throwsWhenSelfReport() {
            AudioStream stream = StreamFixtures.stream(LIVE_ID, BROADCASTER_ID, BAND_ID, StreamStatus.OPEN);
            when(audioStreamRepository.findById(LIVE_ID)).thenReturn(Optional.of(stream));

            assertStreamError(
                    () -> streamService.reportUser(broadcaster, LIVE_ID, request(BROADCASTER_ID)),
                    StreamErrorCode.SELF_REPORT_NOT_ALLOWED
            );

            verify(userPort, never()).findAllByIds(anyCollection());
            verify(reportHistoryRepository, never()).save(any(ReportHistory.class));
        }

        @Test
        @DisplayName("신고 대상 유저가 없으면 REPORT_TARGET_NOT_FOUND (참조 프록시 대신 실조회로 검증)")
        void throwsWhenTargetUserMissing() {
            AudioStream stream = StreamFixtures.stream(LIVE_ID, BROADCASTER_ID, BAND_ID, StreamStatus.OPEN);
            when(audioStreamRepository.findById(LIVE_ID)).thenReturn(Optional.of(stream));
            when(userPort.findAllByIds(Set.of(TARGET_ID))).thenReturn(List.of());

            assertStreamError(
                    () -> streamService.reportUser(broadcaster, LIVE_ID, request(TARGET_ID)),
                    StreamErrorCode.REPORT_TARGET_NOT_FOUND
            );

            verify(userPort).findAllByIds(Set.of(TARGET_ID));
            verify(reportHistoryRepository, never()).save(any(ReportHistory.class));
        }

        @Test
        @DisplayName("신고 이력의 모든 필드를 매핑해 저장하고, 디스코드 알림은 커밋 이후에만 발송한다")
        void savesReportAndNotifiesOnlyAfterCommit() {
            TxSyncSupport.begin();

            AudioStream stream = StreamFixtures.stream(LIVE_ID, BROADCASTER_ID, BAND_ID, StreamStatus.OPEN);
            User target = StreamFixtures.fanUser(TARGET_ID);
            ReportHistory saved = ReportHistory.builder()
                    .id(900L)
                    .targetUser(target)
                    .audioStream(stream)
                    .reporterId(BROADCASTER_ID)
                    .reportType(ReportType.ABUSE)
                    .chatMessage("문제 채팅")
                    .comment("상세 코멘트")
                    .build();

            when(audioStreamRepository.findById(LIVE_ID)).thenReturn(Optional.of(stream));
            when(userPort.findAllByIds(Set.of(TARGET_ID))).thenReturn(List.of(target));
            when(reportHistoryRepository.save(any(ReportHistory.class))).thenReturn(saved);

            streamService.reportUser(broadcaster, LIVE_ID, request(TARGET_ID));

            ArgumentCaptor<ReportHistory> captor = ArgumentCaptor.forClass(ReportHistory.class);
            verify(reportHistoryRepository).save(captor.capture());
            ReportHistory report = captor.getValue();
            assertThat(report.getTargetUser()).isSameAs(target);
            assertThat(report.getAudioStream()).isSameAs(stream);
            assertThat(report.getReporterId()).isEqualTo(BROADCASTER_ID);
            assertThat(report.getReportType()).isEqualTo(ReportType.ABUSE);
            assertThat(report.getChatMessage()).isEqualTo("문제 채팅");
            assertThat(report.getComment()).isEqualTo("상세 코멘트");
            assertThat(report.getDiscordNotifiedAt()).isNull();

            // 커밋 전에는 발송되지 않는다
            assertThat(TxSyncSupport.registeredCount()).isEqualTo(1);
            verify(discordMessageSender, never()).sendReportNotificationAsync(anyLong());

            TxSyncSupport.triggerAfterCommit();

            verify(discordMessageSender, times(1)).sendReportNotificationAsync(900L);
        }

        @Test
        @DisplayName("커밋 훅은 저장된 신고 이력의 id로 발송을 요청한다")
        void notifiesWithSavedReportId() {
            TxSyncSupport.begin();

            AudioStream stream = StreamFixtures.stream(LIVE_ID, BROADCASTER_ID, BAND_ID, StreamStatus.OPEN);
            User target = StreamFixtures.fanUser(TARGET_ID);
            when(audioStreamRepository.findById(LIVE_ID)).thenReturn(Optional.of(stream));
            when(userPort.findAllByIds(Set.of(TARGET_ID))).thenReturn(List.of(target));
            when(reportHistoryRepository.save(any(ReportHistory.class)))
                    .thenReturn(ReportHistory.builder().id(4242L).build());

            streamService.reportUser(broadcaster, LIVE_ID, request(TARGET_ID));
            TxSyncSupport.triggerAfterCommit();

            verify(discordMessageSender).sendReportNotificationAsync(eq(4242L));
        }
    }
}
