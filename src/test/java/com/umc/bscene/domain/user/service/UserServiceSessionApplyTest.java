package com.umc.bscene.domain.user.service;

import com.umc.bscene.domain.session.dto.SessionPushMessage;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.user.dto.request.SessionApplyConfirmRequest;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.enums.UserStatus;
import com.umc.bscene.domain.user.exception.UserException;
import com.umc.bscene.domain.user.port.AuthPort;
import com.umc.bscene.domain.user.port.BandPort;
import com.umc.bscene.domain.user.port.FollowPort;
import com.umc.bscene.domain.user.port.NotifyPort;
import com.umc.bscene.domain.user.port.PerformancePort;
import com.umc.bscene.domain.user.port.SessionPort;
import com.umc.bscene.domain.user.dto.response.session.SessionApplicationStatusResult;
import com.umc.bscene.domain.user.repository.FanProfileRepository;
import com.umc.bscene.domain.user.repository.UserGenresRepository;
import com.umc.bscene.domain.user.repository.UserRegionsRepository;
import com.umc.bscene.domain.user.repository.UserRepository;
import com.umc.bscene.domain.user.response.code.UserErrorCode;
import com.umc.bscene.global.notification.enums.NotificationSettingType;
import com.umc.bscene.global.notification.message.PushMessage;
import com.umc.bscene.support.StreamFixtures;
import com.umc.bscene.support.TxSyncSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 밴드 측 세션 지원 수락/거절(decideSessionApply)과
 * 지원자 최종 확정(confirmSessionApply)의 단위 테스트.
 * afterCommit 푸시 발송은 TxSyncSupport로 커밋 시점을 재현해 검증한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 세션 지원 수락·확정")
class UserServiceSessionApplyTest {

    private static final Long SUBMISSION_ID = 77L;
    private static final Long DECIDER_ID = 10L;
    private static final Long APPLICANT_ID = 20L;
    private static final Long BAND_ID = 7L;
    private static final String APPLICATION_NICKNAME = "지원자닉";
    private static final String RECRUITMENT_TITLE = "모집제목";

    @Mock
    private UserRepository userRepository;
    @Mock
    private FanProfileRepository fanProfileRepository;
    @Mock
    private UserGenresRepository userGenresRepository;
    @Mock
    private UserRegionsRepository userRegionsRepository;
    @Mock
    private FollowPort followPort;
    @Mock
    private SessionPort sessionPort;
    @Mock
    private PerformancePort performancePort;
    @Mock
    private BandPort bandPort;
    @Mock
    private AuthPort authPort;
    @Mock
    private NotifyPort notifyPort;

    @Captor
    private ArgumentCaptor<List<Long>> receiverCaptor;
    @Captor
    private ArgumentCaptor<PushMessage> messageCaptor;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository,
                fanProfileRepository,
                userGenresRepository,
                userRegionsRepository,
                followPort,
                sessionPort,
                performancePort,
                bandPort,
                authPort,
                notifyPort
        );
        TxSyncSupport.begin();
    }

    @AfterEach
    void tearDown() {
        TxSyncSupport.end();
    }

    private static SessionApplicationStatusResult statusResult() {
        return new SessionApplicationStatusResult(
                SUBMISSION_ID, BAND_ID, APPLICANT_ID, APPLICATION_NICKNAME, RECRUITMENT_TITLE);
    }

    @Nested
    @DisplayName("decideSessionApply: 밴드 측 수락/거절")
    class DecideSessionApply {

        @Test
        @DisplayName("수락하면 커밋 후 지원자에게 수락 알림, 결정자와 지원자를 뺀 밴드 구성원에게 상태 변경 알림을 보낸다")
        void approveNotifiesApplicantAndOtherMembersAfterCommit() {
            given(sessionPort.findBandIdBySessionApplicationSubmission(SUBMISSION_ID)).willReturn(BAND_ID);
            given(sessionPort.decideApplicationSubmission(SUBMISSION_ID, DECIDER_ID, true))
                    .willReturn(statusResult());
            given(userRepository.findById(APPLICANT_ID))
                    .willReturn(Optional.of(StreamFixtures.fanUser(APPLICANT_ID)));
            given(bandPort.getAcceptedMemberUserIds(BAND_ID))
                    .willReturn(List.of(DECIDER_ID, APPLICANT_ID, 30L, 30L, 40L));

            userService.decideSessionApply(DECIDER_ID, SUBMISSION_ID, true);

            verify(bandPort).validateActiveBandMember(DECIDER_ID, BAND_ID);
            verifyNoInteractions(notifyPort);

            TxSyncSupport.triggerAfterCommit();

            verify(notifyPort, times(2)).notify(receiverCaptor.capture(), messageCaptor.capture());
            List<List<Long>> receivers = receiverCaptor.getAllValues();
            List<PushMessage> messages = messageCaptor.getAllValues();

            assertThat(receivers.get(0)).containsExactly(APPLICANT_ID);
            SessionPushMessage applicantMessage = (SessionPushMessage) messages.get(0);
            assertThat(applicantMessage.settingType())
                    .isEqualTo(NotificationSettingType.FAN_SESSION_APPLICATION_STATUS);
            assertThat(applicantMessage.title()).isEqualTo("세션 지원이 수락되었어요");
            assertThat(applicantMessage.body()).isEqualTo("'" + RECRUITMENT_TITLE + "' 모집에서 지원을 수락했어요.");
            assertThat(applicantMessage.body()).doesNotContain("최종 참여 여부");
            assertThat(applicantMessage.referenceId()).isEqualTo(SUBMISSION_ID);

            assertThat(receivers.get(1)).containsExactly(30L, 40L);
            SessionPushMessage bandMessage = (SessionPushMessage) messages.get(1);
            assertThat(bandMessage.settingType())
                    .isEqualTo(NotificationSettingType.BAND_SESSION_APPLICATION_STATUS);
            assertThat(bandMessage.body()).contains(APPLICATION_NICKNAME, "수락했어요");

            verify(bandPort, never())
                    .registerSessionMember(
                            anyLong(),
                            any(User.class),
                            any(),
                            any()
                    );
        }

        @Test
        @DisplayName("거절하면 지원자 계정 상태를 확인하지 않고 거절 알림만 보낸다")
        void rejectSkipsApplicantStatusGuard() {
            given(sessionPort.findBandIdBySessionApplicationSubmission(SUBMISSION_ID)).willReturn(BAND_ID);
            given(sessionPort.decideApplicationSubmission(SUBMISSION_ID, DECIDER_ID, false))
                    .willReturn(statusResult());
            given(bandPort.getAcceptedMemberUserIds(BAND_ID)).willReturn(List.of(30L));

            userService.decideSessionApply(DECIDER_ID, SUBMISSION_ID, false);

            verifyNoInteractions(userRepository);

            TxSyncSupport.triggerAfterCommit();

            verify(notifyPort, times(2)).notify(receiverCaptor.capture(), messageCaptor.capture());
            SessionPushMessage applicantMessage = (SessionPushMessage) messageCaptor.getAllValues().get(0);
            assertThat(applicantMessage.title()).isEqualTo("세션 지원이 거절되었어요");
            SessionPushMessage bandMessage = (SessionPushMessage) messageCaptor.getAllValues().get(1);
            assertThat(bandMessage.body()).contains("거절했어요");
        }

        @Test
        @DisplayName("결정자와 지원자 외의 밴드 구성원이 없으면 지원자 알림만 발송된다")
        void approveWithoutOtherMembersNotifiesApplicantOnly() {
            given(sessionPort.findBandIdBySessionApplicationSubmission(SUBMISSION_ID)).willReturn(BAND_ID);
            given(sessionPort.decideApplicationSubmission(SUBMISSION_ID, DECIDER_ID, true))
                    .willReturn(statusResult());
            given(userRepository.findById(APPLICANT_ID))
                    .willReturn(Optional.of(StreamFixtures.fanUser(APPLICANT_ID)));
            given(bandPort.getAcceptedMemberUserIds(BAND_ID))
                    .willReturn(List.of(DECIDER_ID, APPLICANT_ID));

            userService.decideSessionApply(DECIDER_ID, SUBMISSION_ID, true);
            TxSyncSupport.triggerAfterCommit();

            verify(notifyPort).notify(receiverCaptor.capture(), messageCaptor.capture());
            assertThat(receiverCaptor.getValue()).containsExactly(APPLICANT_ID);
        }

        @Test
        @DisplayName("수락 시 지원자가 존재하지 않으면 실패하고 커밋 훅이 등록되지 않는다")
        void approveFailsWhenApplicantMissing() {
            given(sessionPort.findBandIdBySessionApplicationSubmission(SUBMISSION_ID)).willReturn(BAND_ID);
            given(sessionPort.decideApplicationSubmission(SUBMISSION_ID, DECIDER_ID, true))
                    .willReturn(statusResult());
            given(userRepository.findById(APPLICANT_ID)).willReturn(Optional.empty());

            UserException exception = assertThrows(
                    UserException.class,
                    () -> userService.decideSessionApply(DECIDER_ID, SUBMISSION_ID, true)
            );

            assertThat(exception.getBaseResponseCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
            assertThat(TxSyncSupport.registeredCount()).isZero();
            verifyNoInteractions(notifyPort);
        }

        @Test
        @DisplayName("수락 시 지원자 계정이 활성 상태가 아니면 실패한다")
        void approveFailsWhenApplicantNotActive() {
            given(sessionPort.findBandIdBySessionApplicationSubmission(SUBMISSION_ID)).willReturn(BAND_ID);
            given(sessionPort.decideApplicationSubmission(SUBMISSION_ID, DECIDER_ID, true))
                    .willReturn(statusResult());
            User suspended = User.builder()
                    .id(APPLICANT_ID)
                    .status(UserStatus.SUSPENDED)
                    .build();
            given(userRepository.findById(APPLICANT_ID)).willReturn(Optional.of(suspended));

            UserException exception = assertThrows(
                    UserException.class,
                    () -> userService.decideSessionApply(DECIDER_ID, SUBMISSION_ID, true)
            );

            assertThat(exception.getBaseResponseCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
            assertThat(TxSyncSupport.registeredCount()).isZero();
        }

        @Test
        @DisplayName("결정자가 해당 밴드의 활성 구성원이 아니면 상태 전이 없이 실패한다")
        void nonMemberDeciderFailsBeforeDecision() {
            given(sessionPort.findBandIdBySessionApplicationSubmission(SUBMISSION_ID)).willReturn(BAND_ID);
            willThrow(new UserException(UserErrorCode.PARAM_BAD_REQUEST))
                    .given(bandPort).validateActiveBandMember(DECIDER_ID, BAND_ID);

            assertThrows(
                    UserException.class,
                    () -> userService.decideSessionApply(DECIDER_ID, SUBMISSION_ID, true)
            );

            verify(sessionPort, never()).decideApplicationSubmission(anyLong(), anyLong(), anyBoolean());
            verifyNoInteractions(notifyPort);
        }
    }

    @Nested
    @DisplayName("confirmSessionApply: 지원자 최종 확정")
    class ConfirmSessionApply {

        @Test
        @DisplayName("최종 수락하면 확정 활동명·파트로 세션 멤버를 등록하고 커밋 후 지원자를 뺀 밴드 구성원에게 알린다")
        void acceptRegistersSessionMemberAndNotifiesBand() {
            User applicant = StreamFixtures.fanUser(APPLICANT_ID);
            given(sessionPort.finalizeApplicationSubmission(SUBMISSION_ID, APPLICANT_ID, true))
                    .willReturn(statusResult());
            given(userRepository.findById(APPLICANT_ID)).willReturn(Optional.of(applicant));
            given(bandPort.getAcceptedMemberUserIds(BAND_ID))
                    .willReturn(List.of(APPLICANT_ID, 30L, 40L));

            userService.confirmSessionApply(
                    APPLICANT_ID, SUBMISSION_ID,
                    new SessionApplyConfirmRequest(true, "확정닉", Part.GUITAR));

            verify(bandPort).registerSessionMember(BAND_ID, applicant, "확정닉", Part.GUITAR);
            verifyNoInteractions(notifyPort);

            TxSyncSupport.triggerAfterCommit();

            verify(notifyPort).notify(receiverCaptor.capture(), messageCaptor.capture());
            assertThat(receiverCaptor.getValue()).containsExactly(30L, 40L);
            SessionPushMessage message = (SessionPushMessage) messageCaptor.getValue();
            assertThat(message.settingType())
                    .isEqualTo(NotificationSettingType.BAND_SESSION_APPLICATION_STATUS);
            assertThat(message.body()).contains("확정닉", "확정했어요");
            assertThat(message.referenceId()).isEqualTo(SUBMISSION_ID);
        }

        @Test
        @DisplayName("최종 거절하면 멤버 등록 없이 지원서의 활동명으로 거절 알림을 보낸다")
        void rejectSkipsRegistrationAndUsesApplicationNickname() {
            given(sessionPort.finalizeApplicationSubmission(SUBMISSION_ID, APPLICANT_ID, false))
                    .willReturn(statusResult());
            given(bandPort.getAcceptedMemberUserIds(BAND_ID)).willReturn(List.of(30L));

            userService.confirmSessionApply(
                    APPLICANT_ID, SUBMISSION_ID,
                    new SessionApplyConfirmRequest(false, null, null));

            verify(bandPort, never()).registerSessionMember(anyLong(), any(User.class), any(), any());
            verifyNoInteractions(userRepository);

            TxSyncSupport.triggerAfterCommit();

            verify(notifyPort).notify(receiverCaptor.capture(), messageCaptor.capture());
            SessionPushMessage message = (SessionPushMessage) messageCaptor.getValue();
            assertThat(message.body()).contains(APPLICATION_NICKNAME, "거절했어요");
        }

        @Test
        @DisplayName("최종 수락인데 활동명이 없으면 상태 전이 없이 실패한다")
        void acceptWithoutNicknameFails() {
            UserException exception = assertThrows(
                    UserException.class,
                    () -> userService.confirmSessionApply(
                            APPLICANT_ID, SUBMISSION_ID,
                            new SessionApplyConfirmRequest(true, null, Part.GUITAR))
            );

            assertThat(exception.getBaseResponseCode()).isEqualTo(UserErrorCode.PARAM_BAD_REQUEST);
            verifyNoInteractions(sessionPort);
        }

        @Test
        @DisplayName("최종 수락인데 활동명이 공백뿐이면 실패한다")
        void acceptWithBlankNicknameFails() {
            UserException exception = assertThrows(
                    UserException.class,
                    () -> userService.confirmSessionApply(
                            APPLICANT_ID, SUBMISSION_ID,
                            new SessionApplyConfirmRequest(true, "   ", Part.GUITAR))
            );

            assertThat(exception.getBaseResponseCode()).isEqualTo(UserErrorCode.PARAM_BAD_REQUEST);
            verifyNoInteractions(sessionPort);
        }

        @Test
        @DisplayName("최종 수락인데 파트가 없으면 실패한다")
        void acceptWithoutPartFails() {
            UserException exception = assertThrows(
                    UserException.class,
                    () -> userService.confirmSessionApply(
                            APPLICANT_ID, SUBMISSION_ID,
                            new SessionApplyConfirmRequest(true, "확정닉", null))
            );

            assertThat(exception.getBaseResponseCode()).isEqualTo(UserErrorCode.PARAM_BAD_REQUEST);
            verifyNoInteractions(sessionPort);
        }

        @Test
        @DisplayName("최종 수락 시 지원자 계정이 없으면 멤버 등록 없이 실패한다")
        void acceptFailsWhenApplicantMissing() {
            given(sessionPort.finalizeApplicationSubmission(SUBMISSION_ID, APPLICANT_ID, true))
                    .willReturn(statusResult());
            given(userRepository.findById(APPLICANT_ID)).willReturn(Optional.empty());

            UserException exception = assertThrows(
                    UserException.class,
                    () -> userService.confirmSessionApply(
                            APPLICANT_ID, SUBMISSION_ID,
                            new SessionApplyConfirmRequest(true, "확정닉", Part.GUITAR))
            );

            assertThat(exception.getBaseResponseCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
            verify(bandPort, never()).registerSessionMember(anyLong(), any(User.class), any(), any());
            assertThat(TxSyncSupport.registeredCount()).isZero();
        }

        @Test
        @DisplayName("지원자 외의 밴드 구성원이 없으면 커밋 훅 등록 없이 알림을 건너뛴다")
        void skipsNotificationWhenNoOtherMembers() {
            User applicant = StreamFixtures.fanUser(APPLICANT_ID);
            given(sessionPort.finalizeApplicationSubmission(SUBMISSION_ID, APPLICANT_ID, true))
                    .willReturn(statusResult());
            given(userRepository.findById(APPLICANT_ID)).willReturn(Optional.of(applicant));
            given(bandPort.getAcceptedMemberUserIds(BAND_ID)).willReturn(List.of(APPLICANT_ID));

            userService.confirmSessionApply(
                    APPLICANT_ID, SUBMISSION_ID,
                    new SessionApplyConfirmRequest(true, "확정닉", Part.GUITAR));

            assertThat(TxSyncSupport.registeredCount()).isZero();

            TxSyncSupport.triggerAfterCommit();
            verifyNoInteractions(notifyPort);
        }
    }
}
