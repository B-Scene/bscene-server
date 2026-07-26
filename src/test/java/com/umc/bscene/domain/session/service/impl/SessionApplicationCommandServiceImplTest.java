package com.umc.bscene.domain.session.service.impl;

import com.umc.bscene.domain.session.dto.application.request.SessionApplicationVisibilityRequest;
import com.umc.bscene.domain.session.entity.SessionApplication;
import com.umc.bscene.domain.session.enums.code.error.SessionErrorCode;
import com.umc.bscene.domain.session.exception.SessionApplicationException;
import com.umc.bscene.domain.session.port.BandMemberPort;
import com.umc.bscene.domain.session.port.NotifyPort;
import com.umc.bscene.domain.session.repository.SessionApplicationRepository;
import com.umc.bscene.domain.session.repository.SessionApplicationSubmissionRepository;
import com.umc.bscene.domain.session.repository.SessionRecruitmentRepository;
import com.umc.bscene.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
}
