package com.umc.bscene.domain.stream.service;

import com.umc.bscene.domain.chat.service.LiveChatRoomCloser;
import com.umc.bscene.domain.stream.dto.StreamPushMessage;
import com.umc.bscene.domain.stream.dto.request.StreamCreateRequest;
import com.umc.bscene.domain.stream.dto.response.BandSummaryResponse;
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
import com.umc.bscene.global.notification.enums.NotificationSettingType;
import com.umc.bscene.global.notification.message.PushMessage;
import com.umc.bscene.global.security.util.JwtUtil;
import com.umc.bscene.support.StreamFixtures;
import com.umc.bscene.support.TxSyncSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * notifyLiveRecipientsAfterCommit(라이브 예약/시작 푸시 팬아웃)의 단위 테스트.
 * 공개 진입점인 createStream(예약 알림)과 enterRoom(시작 알림)을 통해 검증한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StreamServiceImpl 라이브 푸시 팬아웃")
class StreamServiceImplNotificationTest {

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
    private ValueOperations<String, String> valueOperations;
    @Mock
    private ZSetOperations<String, String> zSetOperations;
    @Mock
    private BandMemberPort bandMemberPort;
    @Mock
    private FollowPort followPort;
    @Mock
    private UserTermsPort userTermsPort;
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

    @Captor
    private ArgumentCaptor<List<Long>> receiverCaptor;
    @Captor
    private ArgumentCaptor<PushMessage> messageCaptor;
    @Captor
    private ArgumentCaptor<String> cooldownKeyCaptor;
    @Captor
    private ArgumentCaptor<Duration> cooldownTtlCaptor;

    private StreamServiceImpl streamService;

    private static final Long BROADCASTER_ID = 100L;
    private static final Long BAND_ID = 7L;
    private static final Long LIVE_ID = 55L;
    private static final String BAND_NAME = "밴드이름";
    private static final String LIVE_TITLE = "title-" + LIVE_ID;

    private static final LocalDateTime SCHEDULED_AT = LocalDateTime.of(2026, 7, 11, 21, 0);
    // "M.dd. (E) a h:mm" + Locale.KOREAN, 2026-07-11은 토요일
    private static final String SCHEDULED_AT_TEXT = "7.11. (토) 오후 9:00";

    private static final String SCHEDULED_COOLDOWN_KEY = "livePush:cooldown:" + BAND_ID + ":scheduled";
    private static final String STARTED_COOLDOWN_KEY = "livePush:cooldown:" + BAND_ID + ":started";

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
                userTermsPort,
                notifyPort,
                mtxRestClient,
                viewerSsePresence,
                liveChatRoomCloser,
                discordMessageSender,
                "https://hls.test",
                "https://webrtc.test"
        );
        TxSyncSupport.begin();
    }

    @AfterEach
    void tearDown() {
        TxSyncSupport.end();
    }

    // ---------- 공통 스텁 ----------

    private void givenCooldown(Boolean acquired) {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).willReturn(acquired);
    }

    private void givenBandSummary() {
        given(bandMemberPort.getBandSummaryByBandId(BAND_ID))
                .willReturn(Optional.of(new BandSummaryResponse(BAND_ID, BAND_NAME)));
    }

    private void givenAudience(List<Long> followerIds, List<Long> agreedFollowerIds, List<Long> memberIds) {
        given(followPort.getFollowerUserIdsByBandId(BAND_ID)).willReturn(followerIds);
        given(userTermsPort.filterNotificationAgreedUserIds(followerIds)).willReturn(agreedFollowerIds);
        given(bandMemberPort.getAcceptedMemberUserIds(BAND_ID)).willReturn(memberIds);
    }

    /** createStream(예약 라이브 생성)까지 필요한 스텁을 걸고 호출한다. */
    private void callCreateStream(LocalDateTime scheduledAt) {
        User broadcaster = StreamFixtures.bandUser(BROADCASTER_ID);
        AudioStream saved = StreamFixtures.scheduledStream(LIVE_ID, BROADCASTER_ID, BAND_ID, scheduledAt);

        if (scheduledAt != null)
            given(audioStreamRepository.countByBroadcasterIdAndStatus(BROADCASTER_ID, StreamStatus.SCHEDULED))
                    .willReturn(0L);

        given(bandMemberPort.getBandSummaryByBroadcasterId(BROADCASTER_ID))
                .willReturn(Optional.of(new BandSummaryResponse(BAND_ID, BAND_NAME)));
        given(audioStreamRepository.save(any(AudioStream.class))).willReturn(saved);

        streamService.createStream(
                broadcaster,
                new StreamCreateRequest(LIVE_TITLE, "description", null, scheduledAt, null)
        );
    }

    /** enterRoom에서 SCHEDULED -> OPEN 전환에 성공하도록 스텁을 건다. */
    private void givenScheduledStreamStartedByBroadcaster() {
        given(audioStreamRepository.findById(LIVE_ID))
                .willReturn(Optional.of(StreamFixtures.scheduledStream(LIVE_ID, BROADCASTER_ID, BAND_ID, SCHEDULED_AT)));
        given(bandMemberPort.isActiveRegularMemberOfBand(BAND_ID, BROADCASTER_ID)).willReturn(true);
        given(audioStreamRepository.existsByBroadcasterIdAndStatus(BROADCASTER_ID, StreamStatus.OPEN)).willReturn(false);
        given(audioStreamRepository.markStartedIfScheduled(eq(LIVE_ID), any(LocalDateTime.class))).willReturn(1);
    }

    /** 알림 로직 이후 enterRoom이 응답을 만드는 구간에서 필요한 스텁. */
    private void givenEnterRoomResponseStubs() {
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(bandMemberPort.getBandNameWithBandProfileByBroadcasterId(Set.of(BROADCASTER_ID))).willReturn(List.of());
    }

    private List<StreamPushMessage> capturedMessages() {
        return messageCaptor.getAllValues().stream()
                .map(StreamPushMessage.class::cast)
                .toList();
    }

    @Nested
    @DisplayName("커밋 후 발송 계약")
    class AfterCommitContract {

        @Test
        @DisplayName("예약 알림은 서비스 호출 중에는 발송되지 않고 커밋 이후에만 발송된다")
        void 예약_알림은_커밋_이후에만_발송된다() {
            givenCooldown(true);
            givenBandSummary();
            givenAudience(List.of(201L), List.of(201L), List.of(301L));

            callCreateStream(SCHEDULED_AT);

            verifyNoInteractions(notifyPort);
            assertThat(TxSyncSupport.registeredCount()).isEqualTo(1);

            TxSyncSupport.triggerAfterCommit();

            verify(notifyPort, times(2)).notify(receiverCaptor.capture(), messageCaptor.capture());
        }

        @Test
        @DisplayName("시작 알림은 서비스 호출 중에는 발송되지 않고 커밋 이후에만 발송된다")
        void 시작_알림은_커밋_이후에만_발송된다() {
            givenScheduledStreamStartedByBroadcaster();
            givenCooldown(true);
            givenBandSummary();
            givenAudience(List.of(201L), List.of(201L), List.of(301L));
            givenEnterRoomResponseStubs();

            streamService.enterRoom(BROADCASTER_ID, LIVE_ID);

            verifyNoInteractions(notifyPort);
            assertThat(TxSyncSupport.registeredCount()).isEqualTo(1);

            TxSyncSupport.triggerAfterCommit();

            verify(notifyPort, times(2)).notify(receiverCaptor.capture(), messageCaptor.capture());
        }
    }

    @Nested
    @DisplayName("밴드별 푸시 쿨다운")
    class Cooldown {

        @Test
        @DisplayName("예약 알림 쿨다운 키는 scheduled 접미사, TTL은 5분이다")
        void 예약_쿨다운_키와_TTL() {
            givenCooldown(true);
            givenBandSummary();
            givenAudience(List.of(201L), List.of(201L), List.of());

            callCreateStream(SCHEDULED_AT);

            verify(valueOperations).setIfAbsent(cooldownKeyCaptor.capture(), eq("1"), cooldownTtlCaptor.capture());
            assertThat(cooldownKeyCaptor.getValue()).isEqualTo(SCHEDULED_COOLDOWN_KEY);
            assertThat(cooldownTtlCaptor.getValue()).isEqualTo(Duration.ofMinutes(5));
        }

        @Test
        @DisplayName("시작 알림 쿨다운 키는 started 접미사, TTL은 30분이다")
        void 시작_쿨다운_키와_TTL() {
            givenScheduledStreamStartedByBroadcaster();
            givenCooldown(true);
            givenBandSummary();
            givenAudience(List.of(201L), List.of(201L), List.of());
            givenEnterRoomResponseStubs();

            streamService.enterRoom(BROADCASTER_ID, LIVE_ID);

            verify(valueOperations).setIfAbsent(cooldownKeyCaptor.capture(), eq("1"), cooldownTtlCaptor.capture());
            assertThat(cooldownKeyCaptor.getValue()).isEqualTo(STARTED_COOLDOWN_KEY);
            assertThat(cooldownTtlCaptor.getValue()).isEqualTo(Duration.ofMinutes(30));
        }

        @Test
        @DisplayName("쿨다운 미획득(false)이면 수신자 조회도 커밋 훅 등록도 없다")
        void 쿨다운_미획득이면_수신자_조회도_없다() {
            givenCooldown(false);
            givenBandSummary();

            callCreateStream(SCHEDULED_AT);

            verifyNoInteractions(followPort, userTermsPort, notifyPort);
            verify(bandMemberPort, never()).getAcceptedMemberUserIds(any());
            assertThat(TxSyncSupport.registeredCount()).isZero();
        }

        @Test
        @DisplayName("setIfAbsent가 null을 반환하면 미획득으로 간주해 동일하게 스킵한다")
        void setIfAbsent_null이면_스킵한다() {
            givenCooldown(null);
            givenBandSummary();

            callCreateStream(SCHEDULED_AT);

            verifyNoInteractions(followPort, userTermsPort, notifyPort);
            verify(bandMemberPort, never()).getAcceptedMemberUserIds(any());
            assertThat(TxSyncSupport.registeredCount()).isZero();
        }
    }

    @Nested
    @DisplayName("밴드 요약 조회")
    class BandSummaryLookup {

        @Test
        @DisplayName("밴드 요약이 없으면 쿨다운 키도 세팅하지 않고 즉시 종료한다")
        void 밴드_요약이_없으면_즉시_종료한다() {
            given(bandMemberPort.getBandSummaryByBandId(BAND_ID)).willReturn(Optional.empty());

            callCreateStream(SCHEDULED_AT);

            verify(redisTemplate, never()).opsForValue();
            verifyNoInteractions(followPort, userTermsPort, notifyPort);
            assertThat(TxSyncSupport.registeredCount()).isZero();
        }
    }

    @Nested
    @DisplayName("수신자 집합 계산")
    class RecipientSet {

        @Test
        @DisplayName("알림 수신에 동의한 팔로워만 팬 알림을 받는다")
        void 동의한_팔로워만_수신한다() {
            givenCooldown(true);
            givenBandSummary();
            givenAudience(List.of(201L, 202L, 203L), List.of(201L, 203L), List.of());

            callCreateStream(SCHEDULED_AT);
            TxSyncSupport.triggerAfterCommit();

            verify(notifyPort, times(1)).notify(receiverCaptor.capture(), messageCaptor.capture());
            assertThat(receiverCaptor.getValue()).containsExactly(201L, 203L);
            assertThat(capturedMessages().getFirst().settingType())
                    .isEqualTo(NotificationSettingType.FAN_SCHEDULED_LIVE_REMINDER);
        }

        @Test
        @DisplayName("밴드 구성원은 약관 동의 필터와 무관하게 수신한다")
        void 밴드_구성원은_약관_필터와_무관하다() {
            givenCooldown(true);
            givenBandSummary();
            // 팔로워 전원이 약관 미동의여도 밴드 구성원은 그대로 수신
            givenAudience(List.of(201L), List.of(), List.of(301L, 302L));

            callCreateStream(SCHEDULED_AT);
            TxSyncSupport.triggerAfterCommit();

            verify(notifyPort, times(1)).notify(receiverCaptor.capture(), messageCaptor.capture());
            assertThat(receiverCaptor.getValue()).containsExactly(301L, 302L);
            assertThat(capturedMessages().getFirst().settingType())
                    .isEqualTo(NotificationSettingType.BAND_SCHEDULED_LIVE_REMINDER);
        }

        @Test
        @DisplayName("송출자 본인은 팬 목록과 밴드 목록 모두에서 제외된다")
        void 송출자는_양쪽_모두에서_제외된다() {
            givenCooldown(true);
            givenBandSummary();
            givenAudience(
                    List.of(BROADCASTER_ID, 201L),
                    List.of(BROADCASTER_ID, 201L),
                    List.of(BROADCASTER_ID, 301L)
            );

            callCreateStream(SCHEDULED_AT);
            TxSyncSupport.triggerAfterCommit();

            verify(notifyPort, times(2)).notify(receiverCaptor.capture(), messageCaptor.capture());
            assertThat(receiverCaptor.getAllValues().get(0)).containsExactly(201L);
            assertThat(receiverCaptor.getAllValues().get(1)).containsExactly(301L);
        }

        @Test
        @DisplayName("팔로워이면서 밴드 구성원이면 밴드 알림만 한 번 받는다")
        void 팔로워이자_밴드_구성원은_밴드_알림만_받는다() {
            givenCooldown(true);
            givenBandSummary();
            givenAudience(List.of(201L, 301L), List.of(201L, 301L), List.of(301L, 302L));

            callCreateStream(SCHEDULED_AT);
            TxSyncSupport.triggerAfterCommit();

            verify(notifyPort, times(2)).notify(receiverCaptor.capture(), messageCaptor.capture());
            List<Long> fanReceivers = receiverCaptor.getAllValues().get(0);
            List<Long> bandReceivers = receiverCaptor.getAllValues().get(1);

            assertThat(fanReceivers).containsExactly(201L);
            assertThat(fanReceivers).doesNotContain(301L);
            assertThat(bandReceivers).containsExactly(301L, 302L);
        }

        @Test
        @DisplayName("각 목록 내부의 중복 ID는 하나로 합쳐진다")
        void 목록_내부_중복은_제거된다() {
            givenCooldown(true);
            givenBandSummary();
            givenAudience(
                    List.of(201L, 201L, 202L),
                    List.of(201L, 201L, 202L),
                    List.of(301L, 301L)
            );

            callCreateStream(SCHEDULED_AT);
            TxSyncSupport.triggerAfterCommit();

            verify(notifyPort, times(2)).notify(receiverCaptor.capture(), messageCaptor.capture());
            assertThat(receiverCaptor.getAllValues().get(0)).containsExactly(201L, 202L);
            assertThat(receiverCaptor.getAllValues().get(1)).containsExactly(301L);
        }

        @Test
        @DisplayName("팬 수신자만 있으면 팬 알림 1회만 발송한다")
        void 팬_수신자만_있으면_1회_발송한다() {
            givenCooldown(true);
            givenBandSummary();
            givenAudience(List.of(201L), List.of(201L), List.of(BROADCASTER_ID));

            callCreateStream(SCHEDULED_AT);
            TxSyncSupport.triggerAfterCommit();

            verify(notifyPort, times(1)).notify(receiverCaptor.capture(), messageCaptor.capture());
            assertThat(receiverCaptor.getValue()).containsExactly(201L);
            assertThat(capturedMessages().getFirst().settingType())
                    .isEqualTo(NotificationSettingType.FAN_SCHEDULED_LIVE_REMINDER);
        }

        @Test
        @DisplayName("팬과 밴드 수신자가 모두 비면 발송도 커밋 훅 등록도 없다")
        void 수신자가_모두_비면_발송하지_않는다() {
            givenCooldown(true);
            givenBandSummary();
            givenAudience(List.of(BROADCASTER_ID), List.of(BROADCASTER_ID), List.of(BROADCASTER_ID));

            callCreateStream(SCHEDULED_AT);

            assertThat(TxSyncSupport.registeredCount()).isZero();

            TxSyncSupport.triggerAfterCommit();

            verifyNoInteractions(notifyPort);
        }

        @Test
        @DisplayName("수신자가 많아도 청중별로 정확히 2회만 발송한다 (수신자당 발송 금지)")
        void 청중별로_정확히_2회만_발송한다() {
            givenCooldown(true);
            givenBandSummary();
            givenAudience(
                    List.of(201L, 202L, 203L),
                    List.of(201L, 202L, 203L),
                    List.of(301L, 302L, 303L)
            );

            callCreateStream(SCHEDULED_AT);
            TxSyncSupport.triggerAfterCommit();

            verify(notifyPort, times(2)).notify(receiverCaptor.capture(), messageCaptor.capture());
            assertThat(receiverCaptor.getAllValues().get(0)).containsExactly(201L, 202L, 203L);
            assertThat(receiverCaptor.getAllValues().get(1)).containsExactly(301L, 302L, 303L);
        }
    }

    @Nested
    @DisplayName("푸시 메시지 내용")
    class MessageContent {

        @Test
        @DisplayName("예약 알림의 settingType은 팬/밴드 각각 SCHEDULED_LIVE_REMINDER 계열이다")
        void 예약_알림_settingType() {
            givenCooldown(true);
            givenBandSummary();
            givenAudience(List.of(201L), List.of(201L), List.of(301L));

            callCreateStream(SCHEDULED_AT);
            TxSyncSupport.triggerAfterCommit();

            verify(notifyPort, times(2)).notify(receiverCaptor.capture(), messageCaptor.capture());
            assertThat(capturedMessages())
                    .extracting(StreamPushMessage::settingType)
                    .containsExactly(
                            NotificationSettingType.FAN_SCHEDULED_LIVE_REMINDER,
                            NotificationSettingType.BAND_SCHEDULED_LIVE_REMINDER
                    );
        }

        @Test
        @DisplayName("시작 알림의 settingType은 팬/밴드 각각 LIVE_START 계열이다")
        void 시작_알림_settingType() {
            givenScheduledStreamStartedByBroadcaster();
            givenCooldown(true);
            givenBandSummary();
            givenAudience(List.of(201L), List.of(201L), List.of(301L));
            givenEnterRoomResponseStubs();

            streamService.enterRoom(BROADCASTER_ID, LIVE_ID);
            TxSyncSupport.triggerAfterCommit();

            verify(notifyPort, times(2)).notify(receiverCaptor.capture(), messageCaptor.capture());
            assertThat(capturedMessages())
                    .extracting(StreamPushMessage::settingType)
                    .containsExactly(
                            NotificationSettingType.FAN_FOLLOWED_BAND_LIVE_START,
                            NotificationSettingType.BAND_LIVE_START_STATUS
                    );
        }

        @Test
        @DisplayName("팬 메시지와 밴드 메시지는 settingType만 다르다")
        void 팬_메시지와_밴드_메시지는_settingType만_다르다() {
            givenCooldown(true);
            givenBandSummary();
            givenAudience(List.of(201L), List.of(201L), List.of(301L));

            callCreateStream(SCHEDULED_AT);
            TxSyncSupport.triggerAfterCommit();

            verify(notifyPort, times(2)).notify(receiverCaptor.capture(), messageCaptor.capture());
            StreamPushMessage fanMessage = capturedMessages().get(0);
            StreamPushMessage bandMessage = capturedMessages().get(1);

            assertThat(fanMessage.settingType()).isNotEqualTo(bandMessage.settingType());
            assertThat(new StreamPushMessage(
                    fanMessage.type(),
                    bandMessage.settingType(),
                    fanMessage.title(),
                    fanMessage.body(),
                    fanMessage.deepLink(),
                    fanMessage.referenceId()
            )).isEqualTo(bandMessage);
        }

        @Test
        @DisplayName("예약 메시지 본문에 'M.dd. (E) a h:mm' 한국어 포맷의 예약 시각이 들어간다")
        void 예약_메시지에_포맷된_예약_시각이_들어간다() {
            givenCooldown(true);
            givenBandSummary();
            givenAudience(List.of(201L), List.of(201L), List.of());

            callCreateStream(SCHEDULED_AT);
            TxSyncSupport.triggerAfterCommit();

            verify(notifyPort, times(1)).notify(receiverCaptor.capture(), messageCaptor.capture());
            StreamPushMessage fanMessage = capturedMessages().getFirst();

            assertThat(fanMessage.body()).startsWith(SCHEDULED_AT_TEXT);
            assertThat(fanMessage.body())
                    .isEqualTo(SCHEDULED_AT_TEXT + "에 '" + LIVE_TITLE + "' 라이브가 시작될 예정이에요.");
            assertThat(fanMessage.title()).isEqualTo(BAND_NAME + " 라이브가 예약됐어요");
        }

        @Test
        @DisplayName("referenceId와 deepLink는 라이브 id를 담는다")
        void referenceId와_deepLink는_라이브_id를_담는다() {
            givenCooldown(true);
            givenBandSummary();
            givenAudience(List.of(201L), List.of(201L), List.of(301L));

            callCreateStream(SCHEDULED_AT);
            TxSyncSupport.triggerAfterCommit();

            verify(notifyPort, times(2)).notify(receiverCaptor.capture(), messageCaptor.capture());
            assertThat(capturedMessages())
                    .allSatisfy(message -> {
                        assertThat(message.referenceId()).isEqualTo(LIVE_ID);
                        assertThat(message.deepLink()).isEqualTo("/lives/" + LIVE_ID);
                    });
        }
    }

    @Nested
    @DisplayName("알림이 발생하지 않는 진입 경로")
    class NoNotificationPaths {

        @Test
        @DisplayName("scheduledAt이 null인 즉시 라이브 생성은 알림 로직에 진입하지 않는다")
        void 즉시_라이브_생성은_알림에_진입하지_않는다() {
            callCreateStream(null);

            verify(bandMemberPort, never()).getBandSummaryByBandId(any());
            verify(redisTemplate, never()).opsForValue();
            verifyNoInteractions(followPort, userTermsPort, notifyPort);
            assertThat(TxSyncSupport.registeredCount()).isZero();
        }

        @Test
        @DisplayName("SCHEDULED -> OPEN 전환에 실패하면 예외가 발생하고 아무것도 발송되지 않는다")
        void 전환_실패시_아무것도_발송되지_않는다() {
            given(audioStreamRepository.findById(LIVE_ID))
                    .willReturn(Optional.of(StreamFixtures.scheduledStream(LIVE_ID, BROADCASTER_ID, BAND_ID, SCHEDULED_AT)));
            given(bandMemberPort.isActiveRegularMemberOfBand(BAND_ID, BROADCASTER_ID)).willReturn(true);
            given(audioStreamRepository.existsByBroadcasterIdAndStatus(BROADCASTER_ID, StreamStatus.OPEN)).willReturn(false);
            given(audioStreamRepository.markStartedIfScheduled(eq(LIVE_ID), any(LocalDateTime.class))).willReturn(0);

            assertThatThrownBy(() -> streamService.enterRoom(BROADCASTER_ID, LIVE_ID))
                    .isInstanceOf(StreamException.class)
                    .extracting(exception -> ((StreamException) exception).getBaseResponseCode())
                    .isEqualTo(StreamErrorCode.AUDIO_STREAM_NOT_SCHEDULED);

            verify(bandMemberPort, never()).getBandSummaryByBandId(any());
            verifyNoInteractions(followPort, userTermsPort, notifyPort);
            assertThat(TxSyncSupport.registeredCount()).isZero();
        }

        @Test
        @DisplayName("이미 OPEN인 라이브에 송출자가 재입장하면 시작 알림을 보내지 않는다")
        void 이미_OPEN인_라이브_재입장은_알림이_없다() {
            given(audioStreamRepository.findById(LIVE_ID))
                    .willReturn(Optional.of(StreamFixtures.stream(LIVE_ID, BROADCASTER_ID, BAND_ID, StreamStatus.OPEN)));
            givenEnterRoomResponseStubs();

            streamService.enterRoom(BROADCASTER_ID, LIVE_ID);

            verify(audioStreamRepository, never()).markStartedIfScheduled(any(), any());
            verify(bandMemberPort, never()).getBandSummaryByBandId(any());
            verify(redisTemplate, never()).opsForValue();
            verifyNoInteractions(followPort, userTermsPort, notifyPort);
            assertThat(TxSyncSupport.registeredCount()).isZero();
        }
    }

    @Nested
    @DisplayName("공동 진행자 초대 수락·거절")
    class CoHostInvitationDecision {

        private static final Long CO_HOST_ID = 200L;
        private static final Long INVITATION_ID = 300L;

        private AudioStream scheduledStream() {
            return StreamFixtures.scheduledStream(
                    LIVE_ID,
                    BROADCASTER_ID,
                    BAND_ID,
                    SCHEDULED_AT
            );
        }

        private StreamMember invitation(
                AudioStream stream,
                StreamMemberStatus status
        ) {
            return StreamFixtures.member(
                    INVITATION_ID,
                    StreamFixtures.bandUser(CO_HOST_ID),
                    stream,
                    status
            );
        }

        @Test
        @DisplayName("초대 기록이 없으면 CO_HOST_INVITATION_NOT_FOUND")
        void 초대기록이_없으면_실패한다() {
            given(streamMemberRepository
                    .findWithStreamByLiveIdAndUserId(
                            LIVE_ID,
                            CO_HOST_ID
                    ))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    streamService.decideCoHostInvitation(
                            CO_HOST_ID,
                            LIVE_ID,
                            true
                    ))
                    .isInstanceOf(StreamException.class)
                    .extracting(exception ->
                            ((StreamException) exception)
                                    .getBaseResponseCode())
                    .isEqualTo(
                            StreamErrorCode
                                    .CO_HOST_INVITATION_NOT_FOUND
                    );

            verify(streamMemberRepository, never())
                    .transitionStatus(any(), any(), any());
            verifyNoInteractions(notifyPort);
        }

        @Test
        @DisplayName("송출자 자신의 멤버 행은 공동 진행자 초대로 처리하지 않는다")
        void 송출자_자신의_행이면_실패한다() {
            AudioStream stream = scheduledStream();
            StreamMember broadcasterRow = StreamFixtures.member(
                    INVITATION_ID,
                    StreamFixtures.bandUser(BROADCASTER_ID),
                    stream,
                    StreamMemberStatus.INVITED
            );
            given(streamMemberRepository
                    .findWithStreamByLiveIdAndUserId(
                            LIVE_ID,
                            BROADCASTER_ID
                    ))
                    .willReturn(Optional.of(broadcasterRow));

            assertThatThrownBy(() ->
                    streamService.decideCoHostInvitation(
                            BROADCASTER_ID,
                            LIVE_ID,
                            true
                    ))
                    .isInstanceOf(StreamException.class)
                    .extracting(exception ->
                            ((StreamException) exception)
                                    .getBaseResponseCode())
                    .isEqualTo(
                            StreamErrorCode
                                    .CO_HOST_INVITATION_NOT_FOUND
                    );

            verify(streamMemberRepository, never())
                    .transitionStatus(any(), any(), any());
        }

        @ParameterizedTest(name = "{0} 상태는 재처리할 수 없다")
        @EnumSource(
                value = StreamMemberStatus.class,
                names = {"ACCEPTED", "REJECTED"}
        )
        void 이미_처리된_초대이면_실패한다(
                StreamMemberStatus status
        ) {
            AudioStream stream = scheduledStream();
            given(streamMemberRepository
                    .findWithStreamByLiveIdAndUserId(
                            LIVE_ID,
                            CO_HOST_ID
                    ))
                    .willReturn(Optional.of(
                            invitation(stream, status)
                    ));

            assertThatThrownBy(() ->
                    streamService.decideCoHostInvitation(
                            CO_HOST_ID,
                            LIVE_ID,
                            true
                    ))
                    .isInstanceOf(StreamException.class)
                    .extracting(exception ->
                            ((StreamException) exception)
                                    .getBaseResponseCode())
                    .isEqualTo(
                            StreamErrorCode
                                    .CO_HOST_INVITATION_ALREADY_PROCESSED
                    );

            verify(streamMemberRepository, never())
                    .transitionStatus(any(), any(), any());
        }

        @Test
        @DisplayName("예약 라이브가 아니면 초대에 응답할 수 없다")
        void 예약상태가_아니면_실패한다() {
            AudioStream openStream = StreamFixtures.stream(
                    LIVE_ID,
                    BROADCASTER_ID,
                    BAND_ID,
                    StreamStatus.OPEN
            );
            given(streamMemberRepository
                    .findWithStreamByLiveIdAndUserId(
                            LIVE_ID,
                            CO_HOST_ID
                    ))
                    .willReturn(Optional.of(invitation(
                            openStream,
                            StreamMemberStatus.INVITED
                    )));

            assertThatThrownBy(() ->
                    streamService.decideCoHostInvitation(
                            CO_HOST_ID,
                            LIVE_ID,
                            true
                    ))
                    .isInstanceOf(StreamException.class)
                    .extracting(exception ->
                            ((StreamException) exception)
                                    .getBaseResponseCode())
                    .isEqualTo(
                            StreamErrorCode.AUDIO_STREAM_NOT_SCHEDULED
                    );

            verify(streamMemberRepository, never())
                    .transitionStatus(any(), any(), any());
        }

        @Test
        @DisplayName("상태 전이 경합에서 갱신 건수가 0이면 이미 처리된 초대로 응답한다")
        void 원자적_상태전이에_실패하면_재처리_예외() {
            AudioStream stream = scheduledStream();
            given(streamMemberRepository
                    .findWithStreamByLiveIdAndUserId(
                            LIVE_ID,
                            CO_HOST_ID
                    ))
                    .willReturn(Optional.of(invitation(
                            stream,
                            StreamMemberStatus.INVITED
                    )));
            given(streamMemberRepository.transitionStatus(
                    INVITATION_ID,
                    StreamMemberStatus.INVITED,
                    StreamMemberStatus.ACCEPTED
            )).willReturn(0);

            assertThatThrownBy(() ->
                    streamService.decideCoHostInvitation(
                            CO_HOST_ID,
                            LIVE_ID,
                            true
                    ))
                    .isInstanceOf(StreamException.class)
                    .extracting(exception ->
                            ((StreamException) exception)
                                    .getBaseResponseCode())
                    .isEqualTo(
                            StreamErrorCode
                                    .CO_HOST_INVITATION_ALREADY_PROCESSED
                    );

            assertThat(TxSyncSupport.registeredCount()).isZero();
            verifyNoInteractions(notifyPort);
        }

        @Test
        @DisplayName("수락하면 ACCEPTED로 전이하고 커밋 후 송출자에게 밴드 활동명으로 알린다")
        void 수락하면_송출자에게_결과알림을_보낸다() {
            AudioStream stream = scheduledStream();
            given(streamMemberRepository
                    .findWithStreamByLiveIdAndUserId(
                            LIVE_ID,
                            CO_HOST_ID
                    ))
                    .willReturn(Optional.of(invitation(
                            stream,
                            StreamMemberStatus.INVITED
                    )));
            given(streamMemberRepository.transitionStatus(
                    INVITATION_ID,
                    StreamMemberStatus.INVITED,
                    StreamMemberStatus.ACCEPTED
            )).willReturn(1);
            given(bandMemberPort.getBandMemberNickname(
                    BAND_ID,
                    CO_HOST_ID
            )).willReturn(Optional.of("활동명"));

            streamService.decideCoHostInvitation(
                    CO_HOST_ID,
                    LIVE_ID,
                    true
            );

            verifyNoInteractions(notifyPort);
            assertThat(TxSyncSupport.registeredCount()).isEqualTo(1);

            TxSyncSupport.triggerAfterCommit();

            verify(notifyPort).notify(
                    receiverCaptor.capture(),
                    messageCaptor.capture()
            );
            assertThat(receiverCaptor.getValue())
                    .containsExactly(BROADCASTER_ID);
            StreamPushMessage message =
                    (StreamPushMessage) messageCaptor.getValue();
            assertThat(message.settingType()).isEqualTo(
                    NotificationSettingType
                            .BAND_LIVE_CO_HOST_INVITATION
            );
            assertThat(message.body())
                    .contains("활동명", LIVE_TITLE, "수락했어요");
            assertThat(message.deepLink())
                    .isEqualTo("/lives/" + LIVE_ID + "/reservation");
            assertThat(message.referenceId()).isEqualTo(LIVE_ID);
        }

        @Test
        @DisplayName("거절하면 REJECTED로 전이하고 활동명이 없을 때 유저 이름으로 알린다")
        void 거절하면_송출자에게_결과알림을_보낸다() {
            AudioStream stream = scheduledStream();
            StreamMember invitation = invitation(
                    stream,
                    StreamMemberStatus.INVITED
            );
            given(streamMemberRepository
                    .findWithStreamByLiveIdAndUserId(
                            LIVE_ID,
                            CO_HOST_ID
                    ))
                    .willReturn(Optional.of(invitation));
            given(streamMemberRepository.transitionStatus(
                    INVITATION_ID,
                    StreamMemberStatus.INVITED,
                    StreamMemberStatus.REJECTED
            )).willReturn(1);
            given(bandMemberPort.getBandMemberNickname(
                    BAND_ID,
                    CO_HOST_ID
            )).willReturn(Optional.empty());

            streamService.decideCoHostInvitation(
                    CO_HOST_ID,
                    LIVE_ID,
                    false
            );
            TxSyncSupport.triggerAfterCommit();

            verify(notifyPort).notify(
                    receiverCaptor.capture(),
                    messageCaptor.capture()
            );
            assertThat(receiverCaptor.getValue())
                    .containsExactly(BROADCASTER_ID);
            StreamPushMessage message =
                    (StreamPushMessage) messageCaptor.getValue();
            assertThat(message.body()).contains(
                    invitation.getUser().getName(),
                    LIVE_TITLE,
                    "거절했어요"
            );
        }
    }
}
