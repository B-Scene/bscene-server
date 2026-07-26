package com.umc.bscene.domain.session.service.impl;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.repository.BandRepository;
import com.umc.bscene.domain.session.entity.SessionRecruitment;
import com.umc.bscene.domain.session.entity.SessionRecruitmentView;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.session.enums.SessionRecruitmentSortType;
import com.umc.bscene.domain.session.enums.SkillLevel;
import com.umc.bscene.domain.session.exception.SessionException;
import com.umc.bscene.domain.session.repository.SessionRecruitmentInterestRepository;
import com.umc.bscene.domain.session.repository.SessionRecruitmentRepository;
import com.umc.bscene.domain.session.repository.SessionRecruitmentViewRepository;
import com.umc.bscene.domain.session.service.SessionRecruitmentSearchKeywordService;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionRecruitmentQueryServiceImplTest {

    @Mock
    private SessionRecruitmentRepository recruitmentRepository;
    @Mock
    private SessionRecruitmentInterestRepository interestRepository;
    @Mock
    private SessionRecruitmentViewRepository viewRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SessionRecruitmentSearchKeywordService searchKeywordService;
    @Mock
    private BandRepository bandRepository;

    private SessionRecruitmentQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SessionRecruitmentQueryServiceImpl(
                recruitmentRepository,
                interestRepository,
                viewRepository,
                userRepository,
                searchKeywordService,
                bandRepository
        );
    }

    @Test
    @DisplayName("모집 공고 목록은 검색어를 정규화하고 다음 커서를 계산한다")
    void getRecruitmentsNormalizesKeywordAndCalculatesCursor() {
        SessionRecruitment first = recruitment(10L);
        SessionRecruitment second = recruitment(9L);
        when(recruitmentRepository.findLatestRecruitments(
                any(), eq(Part.GUITAR), eq(SkillLevel.INTERMEDIATE),
                eq(Genre.HARD_ROCK), eq(Region.SEOUL), eq("기타"),
                eq(null), any(Pageable.class)
        )).thenReturn(List.of(first, second));
        when(interestRepository.findInterestedRecruitmentIds(
                1L, List.of(10L)
        )).thenReturn(Set.of(10L));

        var response = service.getSessionRecruitments(
                1L, Part.GUITAR, SkillLevel.INTERMEDIATE,
                Genre.HARD_ROCK, Region.SEOUL, "  기타  ",
                SessionRecruitmentSortType.LATEST, null, 1
        );

        verify(searchKeywordService).record(1L, "기타");
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getIsInterested()).isTrue();
        assertThat(response.getHasNext()).isTrue();
        assertThat(response.getNextCursor()).isEqualTo(10L);
    }

    @Test
    @DisplayName("정렬값과 크기가 없으면 최신순 10개를 기본값으로 사용한다")
    void getRecruitmentsUsesDefaults() {
        when(recruitmentRepository.findLatestRecruitments(
                any(), eq(null), eq(null), eq(null), eq(null), eq(null),
                eq(null), any(Pageable.class)
        )).thenReturn(List.of());

        var response = service.getSessionRecruitments(
                1L, null, null, null, null,
                " ", null, null, null
        );

        verify(searchKeywordService).record(1L, null);
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.getContent()).isEmpty();
        assertThat(response.getHasNext()).isFalse();
        assertThat(response.getNextCursor()).isNull();
    }

    @Test
    @DisplayName("모집 공고 상세 조회 시 기존 조회 기록을 교체한다")
    void getRecruitmentDetailReplacesPreviousView() {
        SessionRecruitment recruitment = recruitment(10L);
        SessionRecruitmentView previousView = SessionRecruitmentView.builder().build();
        User user = User.builder().id(1L).build();
        when(recruitmentRepository
                .findBySessionRecruitmentIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(recruitment));
        when(viewRepository
                .findBySessionRecruitment_SessionRecruitmentIdAndUser_Id(10L, 1L))
                .thenReturn(Optional.of(previousView));
        when(userRepository.getReferenceById(1L)).thenReturn(user);

        var response = service.getSessionRecruitmentDetail(1L, 10L);

        verify(viewRepository).delete(previousView);
        verify(viewRepository).flush();
        ArgumentCaptor<SessionRecruitmentView> captor =
                ArgumentCaptor.forClass(SessionRecruitmentView.class);
        verify(viewRepository).save(captor.capture());
        assertThat(captor.getValue().getSessionRecruitment()).isSameAs(recruitment);
        assertThat(captor.getValue().getUser()).isSameAs(user);
        assertThat(response.getSessionRecruitmentId()).isEqualTo(10L);
        assertThat(response.getBandName()).isEqualTo("테스트 밴드");
    }

    @Test
    @DisplayName("존재하지 않는 모집 공고를 상세 조회하면 실패한다")
    void getRecruitmentDetailFailsWhenNotFound() {
        when(recruitmentRepository
                .findBySessionRecruitmentIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSessionRecruitmentDetail(1L, 10L))
                .isInstanceOf(SessionException.class);
    }

    @Test
    @DisplayName("모집 공고는 생성 후 3일이 되기 전까지만 신규 공고이다")
    void isNewRecruitmentUsesThreeDayBoundary() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 11, 10, 0);

        assertThat(SessionRecruitmentQueryServiceImpl.isNewRecruitment(
                createdAt,
                createdAt.plusDays(3).minusNanos(1)
        )).isTrue();
        assertThat(SessionRecruitmentQueryServiceImpl.isNewRecruitment(
                createdAt,
                createdAt.plusDays(3)
        )).isFalse();
        assertThat(SessionRecruitmentQueryServiceImpl.isNewRecruitment(
                createdAt,
                createdAt.plusDays(4)
        )).isFalse();
    }

    @Test
    @DisplayName("생성 시각이 없으면 신규 공고가 아니다")
    void isNewRecruitmentReturnsFalseWithoutCreatedAt() {
        assertThat(SessionRecruitmentQueryServiceImpl.isNewRecruitment(
                null,
                LocalDateTime.of(2026, 7, 11, 10, 0)
        )).isFalse();
    }

    private SessionRecruitment recruitment(Long id) {
        Band band = Band.builder()
                .id(20L)
                .owner(User.builder().id(2L).build())
                .name("테스트 밴드")
                .genre(Genre.HARD_ROCK)
                .region(Region.SEOUL)
                .build();
        SessionRecruitment recruitment = SessionRecruitment.builder()
                .sessionRecruitmentId(id)
                .band(band)
                .recruitmentTitle("기타 세션 모집")
                .summary("주 1회 합주")
                .content("상세 내용")
                .part(Part.GUITAR)
                .skillLevel(SkillLevel.INTERMEDIATE)
                .genre(Genre.HARD_ROCK)
                .region(Region.SEOUL)
                .practiceSchedule("매주 토요일")
                .practicePlace("서울")
                .deadlineAt(LocalDateTime.now().plusDays(7))
                .qualification("합주 경험")
                .build();
        ReflectionTestUtils.setField(
                recruitment,
                "createdAt",
                LocalDateTime.now().minusDays(1)
        );
        return recruitment;
    }
}
