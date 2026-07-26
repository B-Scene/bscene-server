package com.umc.bscene.domain.session.service.impl;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.session.entity.SessionApplication;
import com.umc.bscene.domain.session.entity.SessionBasicProfile;
import com.umc.bscene.domain.session.enums.ApplicationStatus;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.session.enums.SkillLevel;
import com.umc.bscene.domain.session.enums.code.error.SessionErrorCode;
import com.umc.bscene.domain.session.exception.SessionApplicationException;
import com.umc.bscene.domain.session.repository.SessionApplicationRepository;
import com.umc.bscene.domain.session.repository.SessionApplicationSubmissionRepository;
import com.umc.bscene.domain.session.repository.SessionBasicProfileRepository;
import com.umc.bscene.domain.session.service.SessionRecruitmentSearchKeywordService;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.repository.UserRepository;
import com.umc.bscene.global.exception.BaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionApplicationQueryServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final String DEFAULT_PURPOSE = "기본";

    @Mock
    private SessionApplicationRepository applicationRepository;
    @Mock
    private SessionApplicationSubmissionRepository submissionRepository;
    @Mock
    private SessionBasicProfileRepository basicProfileRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SessionRecruitmentSearchKeywordService searchKeywordService;

    private SessionApplicationQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SessionApplicationQueryServiceImpl(
                applicationRepository,
                submissionRepository,
                basicProfileRepository,
                userRepository,
                searchKeywordService
        );
    }

    @Test
    @DisplayName("검색 조건이 있으면 추천 기준 없이 세션 지원서를 검색한다")
    void searchWithExplicitCondition() {
        SessionApplication first = application(20L, 2L, DEFAULT_PURPOSE);
        SessionApplication second = application(19L, 3L, DEFAULT_PURPOSE);
        when(applicationRepository.searchDefaultApplications(
                eq(USER_ID), eq(DEFAULT_PURPOSE), eq(Region.SEOUL),
                eq(SkillLevel.INTERMEDIATE), eq(Part.GUITAR),
                eq(Genre.HARD_ROCK), eq(false), eq(null), eq(null),
                eq("기타"), eq(null), any(Pageable.class)
        )).thenReturn(List.of(first, second));
        when(basicProfileRepository.findAllByUser_IdIn(List.of(2L)))
                .thenReturn(List.of());
        when(userRepository.findAllById(List.of(2L)))
                .thenReturn(List.of(User.builder().id(2L).name("검색 사용자").build()));

        var response = service.searchDefaultApplications(
                USER_ID, Region.SEOUL, SkillLevel.INTERMEDIATE,
                Part.GUITAR, Genre.HARD_ROCK, "  기타  ", null, 1
        );

        verify(searchKeywordService).record(USER_ID, "기타");
        verify(applicationRepository, never()).existsRecommendedApplications(
                any(), any(), any(), any()
        );
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getNickname()).isEqualTo("검색 사용자");
        assertThat(response.getHasNext()).isTrue();
        assertThat(response.getNextCursor()).isEqualTo(20L);
    }

    @Test
    @DisplayName("검색 조건이 없고 내 기본 지원서가 있으면 장르와 지역 추천을 적용한다")
    void searchUsesRecommendationFromMyDefaultApplication() {
        SessionApplication myDefault = application(1L, USER_ID, DEFAULT_PURPOSE);
        when(applicationRepository
                .findFirstByUserIdAndPurposeAndDeletedAtIsNullOrderBySessionApplicationIdDesc(
                        USER_ID, DEFAULT_PURPOSE
                )).thenReturn(Optional.of(myDefault));
        when(applicationRepository.existsRecommendedApplications(
                USER_ID, DEFAULT_PURPOSE, Genre.HARD_ROCK, Region.SEOUL
        )).thenReturn(true);
        when(applicationRepository.searchDefaultApplications(
                eq(USER_ID), eq(DEFAULT_PURPOSE), eq(null), eq(null), eq(null), eq(null),
                eq(true), eq(Genre.HARD_ROCK), eq(Region.SEOUL),
                eq(null), eq(null), any(Pageable.class)
        )).thenReturn(List.of());

        var response = service.searchDefaultApplications(
                USER_ID, null, null, null, null, " ", null, null
        );

        verify(searchKeywordService).record(USER_ID, null);
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.getContent()).isEmpty();
        assertThat(response.getHasNext()).isFalse();
    }

    @Test
    @DisplayName("공개된 기본 지원서 상세정보를 조회한다")
    void getDefaultApplicationDetailSuccess() {
        SessionApplication application = application(20L, 2L, DEFAULT_PURPOSE);
        User user = User.builder().id(2L).name("프로필 이름").build();
        SessionBasicProfile profile = SessionBasicProfile.builder()
                .user(user)
                .profileImageUrl("profile.jpg")
                .build();
        when(applicationRepository.findPublicDetailWithPortfolioLinks(
                20L, DEFAULT_PURPOSE
        )).thenReturn(Optional.of(application));
        when(basicProfileRepository.findByUser_Id(2L)).thenReturn(Optional.of(profile));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        var response = service.getDefaultApplicationDetail(20L);

        assertThat(response.getSessionApplicationId()).isEqualTo(20L);
        assertThat(response.getNickname()).isEqualTo("프로필 이름");
        assertThat(response.getProfileImageUrl()).isEqualTo("profile.jpg");
    }

    @Test
    @DisplayName("비공개이거나 기본 지원서가 아니면 상세조회에 실패한다")
    void getDefaultApplicationDetailFailsWhenNotPublicDefault() {
        when(applicationRepository.findPublicDetailWithPortfolioLinks(
                20L, DEFAULT_PURPOSE
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDefaultApplicationDetail(20L))
                .isInstanceOf(SessionApplicationException.class)
                .extracting("baseResponseCode")
                .isEqualTo(SessionErrorCode.SESSION_APPLICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("내 지원서 요약에 지원서와 지원 현황 개수를 반환한다")
    void getMyApplicationSummarySuccess() {
        User user = User.builder().id(USER_ID).name("내 이름").build();
        SessionApplication defaultApplication =
                application(10L, USER_ID, DEFAULT_PURPOSE);
        SessionApplication otherApplication =
                application(11L, USER_ID, "공연 지원");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(basicProfileRepository.findByUser_Id(USER_ID)).thenReturn(Optional.empty());
        when(applicationRepository
                .findFirstByUserIdAndPurposeAndDeletedAtIsNullOrderBySessionApplicationIdDesc(
                        USER_ID, DEFAULT_PURPOSE
                )).thenReturn(Optional.of(defaultApplication));
        when(applicationRepository.countByUserIdAndDeletedAtIsNull(USER_ID)).thenReturn(2L);
        when(submissionRepository.countBySessionApplication_UserId(USER_ID)).thenReturn(5L);
        when(submissionRepository.countBySessionApplication_UserIdAndStatus(
                USER_ID, ApplicationStatus.PENDING
        )).thenReturn(3L);
        when(applicationRepository
                .findAllByUserIdAndDeletedAtIsNullOrderBySessionApplicationIdAsc(USER_ID))
                .thenReturn(List.of(defaultApplication, otherApplication));

        var response = service.getMySessionApplicationSummary(USER_ID);

        assertThat(response.getHasDefaultApplication()).isTrue();
        assertThat(response.getSessionApplicationId()).isEqualTo(10L);
        assertThat(response.getNickname()).isEqualTo("내 이름");
        assertThat(response.getApplicationCount()).isEqualTo(2L);
        assertThat(response.getSubmissionCount()).isEqualTo(5L);
        assertThat(response.getInProgressCount()).isEqualTo(3L);
        assertThat(response.getApplications()).hasSize(2);
    }

    @Test
    @DisplayName("인증 사용자가 없으면 내 지원서 요약 조회에 실패한다")
    void getMyApplicationSummaryFailsWhenUserNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMySessionApplicationSummary(USER_ID))
                .isInstanceOf(BaseException.class);
    }

    private SessionApplication application(Long id, Long userId, String purpose) {
        SessionApplication application = SessionApplication.builder()
                .userId(userId)
                .nickname("기존 닉네임")
                .title("기타 세션 지원서")
                .purpose(purpose)
                .oneLineIntro("기타를 연주합니다")
                .part(Part.GUITAR)
                .skillLevel(SkillLevel.INTERMEDIATE)
                .genre(Genre.HARD_ROCK)
                .region(Region.SEOUL)
                .intro("상세 소개")
                .build();
        ReflectionTestUtils.setField(application, "sessionApplicationId", id);
        return application;
    }
}
