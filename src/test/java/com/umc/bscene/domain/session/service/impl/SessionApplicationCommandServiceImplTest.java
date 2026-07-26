package com.umc.bscene.domain.session.service.impl;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.session.dto.application.request.MySessionApplicationCreateRequest;
import com.umc.bscene.domain.session.dto.application.request.MySessionApplicationUpdateRequest;
import com.umc.bscene.domain.session.dto.application.request.SessionApplicationVisibilityRequest;
import com.umc.bscene.domain.session.entity.SessionApplication;
import com.umc.bscene.domain.session.entity.SessionApplicationSubmission;
import com.umc.bscene.domain.session.entity.SessionRecruitment;
import com.umc.bscene.domain.session.enums.ApplicationStatus;
import com.umc.bscene.domain.session.enums.AvailableActivity;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.session.enums.SkillLevel;
import com.umc.bscene.domain.session.enums.code.error.SessionErrorCode;
import com.umc.bscene.domain.session.exception.SessionApplicationException;
import com.umc.bscene.domain.session.port.BandMemberPort;
import com.umc.bscene.domain.session.port.NotifyPort;
import com.umc.bscene.domain.session.repository.SessionApplicationRepository;
import com.umc.bscene.domain.session.repository.SessionApplicationSubmissionRepository;
import com.umc.bscene.domain.session.repository.SessionRecruitmentRepository;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionApplicationCommandServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long APPLICATION_ID = 10L;
    private static final String DEFAULT_PURPOSE = "기본";

    @Mock
    private SessionApplicationRepository applicationRepository;
    @Mock
    private SessionApplicationSubmissionRepository submissionRepository;
    @Mock
    private SessionRecruitmentRepository recruitmentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private BandMemberPort bandMemberPort;
    @Mock
    private NotifyPort notifyPort;
    @Mock
    private SessionApplicationVisibilityRequest request;
    @Mock
    private MySessionApplicationCreateRequest createRequest;
    @Mock
    private MySessionApplicationUpdateRequest updateRequest;

    private SessionApplicationCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SessionApplicationCommandServiceImpl(
                applicationRepository,
                submissionRepository,
                recruitmentRepository,
                userRepository,
                eventPublisher,
                bandMemberPort,
                notifyPort
        );
    }

    @Test
    @DisplayName("내 기본 지원서를 공개 또는 비공개로 변경한다")
    void updateVisibilitySuccess() {
        SessionApplication application = SessionApplication.builder()
                .userId(USER_ID)
                .nickname("사용자")
                .title("기본 지원서")
                .purpose(DEFAULT_PURPOSE)
                .build();
        when(applicationRepository
                .findBySessionApplicationIdAndUserIdAndDeletedAtIsNull(
                        APPLICATION_ID, USER_ID
                )).thenReturn(Optional.of(application));
        when(request.getIsPublic()).thenReturn(false);

        var response = service.updateVisibility(USER_ID, APPLICATION_ID, request);

        assertThat(application.getIsPublic()).isFalse();
        assertThat(response.sessionApplicationId()).isEqualTo(APPLICATION_ID);
        assertThat(response.isPublic()).isFalse();
    }

    @Test
    @DisplayName("다른 사용자의 지원서 공개 여부는 변경할 수 없다")
    void updateVisibilityFailsWhenApplicationNotOwned() {
        when(applicationRepository
                .findBySessionApplicationIdAndUserIdAndDeletedAtIsNull(
                        APPLICATION_ID, USER_ID
                )).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> service.updateVisibility(USER_ID, APPLICATION_ID, request)
        )
                .isInstanceOf(SessionApplicationException.class)
                .extracting("baseResponseCode")
                .isEqualTo(SessionErrorCode.SESSION_APPLICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("기본 지원서가 아닌 지원서는 공개 여부를 변경할 수 없다")
    void updateVisibilityFailsForNonDefaultApplication() {
        SessionApplication application = SessionApplication.builder()
                .userId(USER_ID)
                .nickname("사용자")
                .title("공연 지원서")
                .purpose("공연 지원")
                .build();
        when(applicationRepository
                .findBySessionApplicationIdAndUserIdAndDeletedAtIsNull(
                        APPLICATION_ID, USER_ID
                )).thenReturn(Optional.of(application));

        assertThatThrownBy(
                () -> service.updateVisibility(USER_ID, APPLICATION_ID, request)
        )
                .isInstanceOf(SessionApplicationException.class)
                .extracting("baseResponseCode")
                .isEqualTo(SessionErrorCode.SESSION_APPLICATION_VISIBILITY_NOT_ALLOWED);
    }

    @Test
    @DisplayName("첫 지원서를 기본 지원서로 생성한다")
    void createDefaultApplicationSuccess() {
        User user = User.builder().id(USER_ID).name("사용자").build();
        givenCreateRequest(DEFAULT_PURPOSE);
        when(applicationRepository.countByUserIdAndDeletedAtIsNull(USER_ID)).thenReturn(0L);
        when(applicationRepository
                .existsByUserIdAndPurposeAndDeletedAtIsNull(USER_ID, DEFAULT_PURPOSE))
                .thenReturn(false);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(applicationRepository.saveAndFlush(any(SessionApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.createSessionApplication(USER_ID, createRequest);

        verify(applicationRepository).saveAndFlush(any(SessionApplication.class));
        assertThat(response.getNickname()).isEqualTo("사용자");
        assertThat(response.getPurpose()).isEqualTo(DEFAULT_PURPOSE);
    }

    @Test
    @DisplayName("첫 지원서가 기본 용도가 아니면 생성할 수 없다")
    void createFailsWhenFirstApplicationIsNotDefault() {
        when(createRequest.getPurpose()).thenReturn("공연 지원");
        when(applicationRepository.countByUserIdAndDeletedAtIsNull(USER_ID)).thenReturn(0L);

        assertThatThrownBy(() -> service.createSessionApplication(USER_ID, createRequest))
                .isInstanceOf(SessionApplicationException.class)
                .extracting("baseResponseCode")
                .isEqualTo(SessionErrorCode.FIRST_SESSION_APPLICATION_MUST_BE_DEFAULT);

        verify(applicationRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("기본 지원서는 사용자별로 하나만 생성할 수 있다")
    void createFailsWhenDefaultApplicationAlreadyExists() {
        when(createRequest.getPurpose()).thenReturn(DEFAULT_PURPOSE);
        when(applicationRepository.countByUserIdAndDeletedAtIsNull(USER_ID))
                .thenReturn(1L);
        when(applicationRepository
                .existsByUserIdAndPurposeAndDeletedAtIsNull(
                        USER_ID, DEFAULT_PURPOSE
                )).thenReturn(true);

        assertThatThrownBy(
                () -> service.createSessionApplication(USER_ID, createRequest)
        )
                .isInstanceOf(SessionApplicationException.class)
                .extracting("baseResponseCode")
                .isEqualTo(SessionErrorCode.DEFAULT_SESSION_APPLICATION_ALREADY_EXISTS);

        verify(applicationRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("내 지원서의 내용을 수정한다")
    void updateApplicationSuccess() {
        SessionApplication application = application(DEFAULT_PURPOSE);
        givenUpdateRequest(DEFAULT_PURPOSE);
        when(applicationRepository.findByIdAndUserIdWithPortfolioLinks(
                APPLICATION_ID, USER_ID
        )).thenReturn(Optional.of(application));
        when(applicationRepository
                .existsByUserIdAndPurposeAndDeletedAtIsNullAndSessionApplicationIdNot(
                        USER_ID, DEFAULT_PURPOSE, APPLICATION_ID
                )).thenReturn(false);
        when(applicationRepository.saveAndFlush(application)).thenReturn(application);

        var response = service.updateSessionApplication(
                USER_ID, APPLICATION_ID, updateRequest
        );

        assertThat(application.getTitle()).isEqualTo("수정한 지원서");
        assertThat(response.getTitle()).isEqualTo("수정한 지원서");
    }

    @Test
    @DisplayName("기본 지원서의 용도는 변경할 수 없다")
    void updateFailsWhenChangingDefaultPurpose() {
        SessionApplication application = application(DEFAULT_PURPOSE);
        when(applicationRepository.findByIdAndUserIdWithPortfolioLinks(
                APPLICATION_ID, USER_ID
        )).thenReturn(Optional.of(application));
        when(updateRequest.getPurpose()).thenReturn("공연 지원");

        assertThatThrownBy(() -> service.updateSessionApplication(
                USER_ID, APPLICATION_ID, updateRequest
        ))
                .isInstanceOf(SessionApplicationException.class)
                .extracting("baseResponseCode")
                .isEqualTo(SessionErrorCode.DEFAULT_SESSION_APPLICATION_PURPOSE_IMMUTABLE);
    }

    @Test
    @DisplayName("다른 지원서를 기본 용도로 변경할 때 기존 기본 지원서와 중복될 수 없다")
    void updateFailsWhenAnotherDefaultApplicationExists() {
        SessionApplication application = application("공연 지원");
        when(applicationRepository.findByIdAndUserIdWithPortfolioLinks(
                APPLICATION_ID, USER_ID
        )).thenReturn(Optional.of(application));
        when(updateRequest.getPurpose()).thenReturn(DEFAULT_PURPOSE);
        when(applicationRepository
                .existsByUserIdAndPurposeAndDeletedAtIsNullAndSessionApplicationIdNot(
                        USER_ID, DEFAULT_PURPOSE, APPLICATION_ID
                )).thenReturn(true);

        assertThatThrownBy(() -> service.updateSessionApplication(
                USER_ID, APPLICATION_ID, updateRequest
        ))
                .isInstanceOf(SessionApplicationException.class)
                .extracting("baseResponseCode")
                .isEqualTo(SessionErrorCode.DEFAULT_SESSION_APPLICATION_ALREADY_EXISTS);

        verify(applicationRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("내 지원서를 삭제하면 제출 내역을 먼저 삭제한다")
    void deleteApplicationSuccess() {
        SessionApplication application = application("공연 지원");
        when(applicationRepository
                .findBySessionApplicationIdAndUserIdAndDeletedAtIsNull(
                        APPLICATION_ID, USER_ID
                )).thenReturn(Optional.of(application));

        service.deleteSessionApplication(USER_ID, APPLICATION_ID);

        var inOrder = org.mockito.Mockito.inOrder(
                submissionRepository, applicationRepository
        );
        inOrder.verify(submissionRepository)
                .deleteAllBySessionApplication_SessionApplicationId(APPLICATION_ID);
        inOrder.verify(applicationRepository).delete(application);
    }

    @Test
    @DisplayName("모집 중이고 본인 공고가 아니면 지원할 수 있다")
    void submitApplicationSuccess() {
        User owner = User.builder().id(2L).build();
        Band band = Band.builder().id(3L).owner(owner).name("밴드").build();
        SessionRecruitment recruitment = SessionRecruitment.builder()
                .sessionRecruitmentId(20L)
                .band(band)
                .recruitmentTitle("기타 모집")
                .deadlineAt(LocalDateTime.now().plusDays(1))
                .build();
        SessionApplication application = application(DEFAULT_PURPOSE);
        when(recruitmentRepository
                .findBySessionRecruitmentIdAndDeletedAtIsNull(20L))
                .thenReturn(Optional.of(recruitment));
        when(applicationRepository
                .findBySessionApplicationIdAndUserIdAndDeletedAtIsNull(
                        APPLICATION_ID, USER_ID
                )).thenReturn(Optional.of(application));
        when(submissionRepository
                .existsBySessionRecruitment_SessionRecruitmentIdAndSessionApplication_SessionApplicationIdAndStatusNot(
                        20L, APPLICATION_ID, ApplicationStatus.CANCELED
                )).thenReturn(false);
        when(submissionRepository.save(any(SessionApplicationSubmission.class)))
                .thenAnswer(invocation -> {
                    SessionApplicationSubmission submission = invocation.getArgument(0);
                    ReflectionTestUtils.setField(
                            submission, "applicationSubmissionId", 30L
                    );
                    return submission;
                });
        when(bandMemberPort.getAcceptedMemberUserIds(3L)).thenReturn(List.of());

        var response = service.submitApplication(USER_ID, 20L, APPLICATION_ID);

        assertThat(response.applicationSubmissionId()).isEqualTo(30L);
        assertThat(response.recruitmentTitle()).isEqualTo("기타 모집");
        assertThat(response.applicationTitle()).isEqualTo("지원서");
    }

    @Test
    @DisplayName("마감된 모집 공고에는 지원할 수 없다")
    void submitFailsWhenRecruitmentClosed() {
        SessionRecruitment recruitment = SessionRecruitment.builder()
                .sessionRecruitmentId(20L)
                .band(Band.builder()
                        .owner(User.builder().id(2L).build())
                        .build())
                .deadlineAt(LocalDateTime.now().minusSeconds(1))
                .build();
        when(recruitmentRepository
                .findBySessionRecruitmentIdAndDeletedAtIsNull(20L))
                .thenReturn(Optional.of(recruitment));

        assertThatThrownBy(
                () -> service.submitApplication(USER_ID, 20L, APPLICATION_ID)
        )
                .isInstanceOf(SessionApplicationException.class)
                .extracting("baseResponseCode")
                .isEqualTo(SessionErrorCode.SESSION_RECRUITMENT_APPLICATION_CLOSED);
    }

    @Test
    @DisplayName("자신이 소유한 밴드의 모집 공고에는 지원할 수 없다")
    void submitFailsForOwnRecruitment() {
        SessionRecruitment recruitment = SessionRecruitment.builder()
                .sessionRecruitmentId(20L)
                .band(Band.builder()
                        .owner(User.builder().id(USER_ID).build())
                        .build())
                .deadlineAt(LocalDateTime.now().plusDays(1))
                .build();
        when(recruitmentRepository
                .findBySessionRecruitmentIdAndDeletedAtIsNull(20L))
                .thenReturn(Optional.of(recruitment));

        assertThatThrownBy(
                () -> service.submitApplication(USER_ID, 20L, APPLICATION_ID)
        )
                .isInstanceOf(SessionApplicationException.class)
                .extracting("baseResponseCode")
                .isEqualTo(SessionErrorCode.SELF_RECRUITMENT_APPLICATION_NOT_ALLOWED);

        verify(applicationRepository, never())
                .findBySessionApplicationIdAndUserIdAndDeletedAtIsNull(
                        any(), any()
                );
    }

    @Test
    @DisplayName("취소되지 않은 동일 지원서의 중복 지원을 방지한다")
    void submitFailsWhenAlreadySubmitted() {
        SessionRecruitment recruitment = openRecruitment();
        SessionApplication application = application(DEFAULT_PURPOSE);
        when(recruitmentRepository
                .findBySessionRecruitmentIdAndDeletedAtIsNull(20L))
                .thenReturn(Optional.of(recruitment));
        when(applicationRepository
                .findBySessionApplicationIdAndUserIdAndDeletedAtIsNull(
                        APPLICATION_ID, USER_ID
                )).thenReturn(Optional.of(application));
        when(submissionRepository
                .existsBySessionRecruitment_SessionRecruitmentIdAndSessionApplication_SessionApplicationIdAndStatusNot(
                        20L, APPLICATION_ID, ApplicationStatus.CANCELED
                )).thenReturn(true);

        assertThatThrownBy(
                () -> service.submitApplication(USER_ID, 20L, APPLICATION_ID)
        )
                .isInstanceOf(SessionApplicationException.class)
                .extracting("baseResponseCode")
                .isEqualTo(SessionErrorCode.SESSION_APPLICATION_ALREADY_SUBMITTED);

        verify(submissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("기존 지원을 취소했다면 동일 지원서로 다시 지원할 수 있다")
    void submitAllowsReapplicationAfterCancellation() {
        SessionRecruitment recruitment = openRecruitment();
        SessionApplication application = application(DEFAULT_PURPOSE);
        when(recruitmentRepository
                .findBySessionRecruitmentIdAndDeletedAtIsNull(20L))
                .thenReturn(Optional.of(recruitment));
        when(applicationRepository
                .findBySessionApplicationIdAndUserIdAndDeletedAtIsNull(
                        APPLICATION_ID, USER_ID
                )).thenReturn(Optional.of(application));
        when(submissionRepository
                .existsBySessionRecruitment_SessionRecruitmentIdAndSessionApplication_SessionApplicationIdAndStatusNot(
                        20L, APPLICATION_ID, ApplicationStatus.CANCELED
                )).thenReturn(false);
        when(submissionRepository.save(any(SessionApplicationSubmission.class)))
                .thenAnswer(invocation -> {
                    SessionApplicationSubmission submission = invocation.getArgument(0);
                    ReflectionTestUtils.setField(
                            submission, "applicationSubmissionId", 31L
                    );
                    return submission;
                });
        when(bandMemberPort.getAcceptedMemberUserIds(3L)).thenReturn(List.of());

        var response = service.submitApplication(
                USER_ID, 20L, APPLICATION_ID
        );

        assertThat(response.applicationSubmissionId()).isEqualTo(31L);
        verify(submissionRepository).save(any(SessionApplicationSubmission.class));
    }

    @Test
    @DisplayName("대기 중인 지원은 취소할 수 있다")
    void cancelSubmissionSuccess() {
        SessionApplicationSubmission submission =
                SessionApplicationSubmission.builder()
                        .status(ApplicationStatus.PENDING)
                        .build();
        when(submissionRepository
                .findByApplicationSubmissionIdAndSessionApplication_UserId(
                        30L, USER_ID
                )).thenReturn(Optional.of(submission));

        service.cancelSubmission(USER_ID, 30L);

        assertThat(submission.getStatus()).isEqualTo(ApplicationStatus.CANCELED);
    }

    @Test
    @DisplayName("대기 상태가 아닌 지원은 취소할 수 없다")
    void cancelSubmissionFailsWhenNotPending() {
        SessionApplicationSubmission submission =
                SessionApplicationSubmission.builder()
                        .status(ApplicationStatus.ACCEPTED)
                        .build();
        when(submissionRepository
                .findByApplicationSubmissionIdAndSessionApplication_UserId(
                        30L, USER_ID
                )).thenReturn(Optional.of(submission));

        assertThatThrownBy(() -> service.cancelSubmission(USER_ID, 30L))
                .isInstanceOf(SessionApplicationException.class)
                .extracting("baseResponseCode")
                .isEqualTo(SessionErrorCode.APPLICATION_SUBMISSION_CANCEL_NOT_ALLOWED);
    }

    private void givenCreateRequest(String purpose) {
        when(createRequest.getTitle()).thenReturn("기본 지원서");
        when(createRequest.getPurpose()).thenReturn(purpose);
        when(createRequest.getOneLineIntro()).thenReturn("기타를 연주합니다");
        when(createRequest.getPart()).thenReturn(Part.GUITAR);
        when(createRequest.getSkillLevel()).thenReturn(SkillLevel.INTERMEDIATE);
        when(createRequest.getGenre()).thenReturn(Genre.HARD_ROCK);
        when(createRequest.getRegion()).thenReturn(Region.SEOUL);
        when(createRequest.getIntro()).thenReturn("상세 소개");
        when(createRequest.getAvailableActivities())
                .thenReturn(List.of(AvailableActivity.values()[0]));
        when(createRequest.getCareers()).thenReturn(null);
        when(createRequest.getPortfolioLinks()).thenReturn(null);
    }

    private void givenUpdateRequest(String purpose) {
        when(updateRequest.getTitle()).thenReturn("수정한 지원서");
        when(updateRequest.getPurpose()).thenReturn(purpose);
        when(updateRequest.getOneLineIntro()).thenReturn("수정 소개");
        when(updateRequest.getPart()).thenReturn(Part.GUITAR);
        when(updateRequest.getSkillLevel()).thenReturn(SkillLevel.ADVANCED);
        when(updateRequest.getGenre()).thenReturn(Genre.HARD_ROCK);
        when(updateRequest.getRegion()).thenReturn(Region.SEOUL);
        when(updateRequest.getIntro()).thenReturn("수정 상세");
        when(updateRequest.getAvailableActivities())
                .thenReturn(List.of(AvailableActivity.values()[0]));
        when(updateRequest.getCareers()).thenReturn(null);
        when(updateRequest.getPortfolioLinks()).thenReturn(null);
    }

    private SessionApplication application(String purpose) {
        SessionApplication application = SessionApplication.builder()
                .userId(USER_ID)
                .nickname("사용자")
                .title("지원서")
                .purpose(purpose)
                .oneLineIntro("소개")
                .part(Part.GUITAR)
                .skillLevel(SkillLevel.INTERMEDIATE)
                .genre(Genre.HARD_ROCK)
                .region(Region.SEOUL)
                .intro("상세")
                .build();
        ReflectionTestUtils.setField(
                application, "sessionApplicationId", APPLICATION_ID
        );
        return application;
    }

    private SessionRecruitment openRecruitment() {
        return SessionRecruitment.builder()
                .sessionRecruitmentId(20L)
                .band(Band.builder()
                        .id(3L)
                        .owner(User.builder().id(2L).build())
                        .name("밴드")
                        .build())
                .recruitmentTitle("기타 모집")
                .deadlineAt(LocalDateTime.now().plusDays(1))
                .build();
    }
}
