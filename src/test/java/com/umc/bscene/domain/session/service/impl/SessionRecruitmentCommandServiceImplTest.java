package com.umc.bscene.domain.session.service.impl;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.entity.BandMember;
import com.umc.bscene.domain.band.exception.BandException;
import com.umc.bscene.domain.band.repository.BandMemberRepository;
import com.umc.bscene.domain.session.dto.recruitment.request.SessionRecruitmentCreateRequest;
import com.umc.bscene.domain.session.entity.SessionRecruitment;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.session.enums.SkillLevel;
import com.umc.bscene.domain.session.enums.code.error.SessionErrorCode;
import com.umc.bscene.domain.session.exception.SessionException;
import com.umc.bscene.domain.session.repository.SessionRecruitmentRepository;
import com.umc.bscene.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionRecruitmentCommandServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long BAND_MEMBER_ID = 10L;

    @Mock
    private SessionRecruitmentRepository recruitmentRepository;
    @Mock
    private BandMemberRepository bandMemberRepository;
    @Mock
    private SessionRecruitmentCreateRequest request;

    private SessionRecruitmentCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SessionRecruitmentCommandServiceImpl(
                recruitmentRepository,
                bandMemberRepository
        );
    }

    @Test
    @DisplayName("밴드 소유자는 미래 마감일로 세션 모집 공고를 등록할 수 있다")
    void createSessionRecruitmentSuccess() {
        LocalDateTime deadlineAt = LocalDateTime.now().plusDays(7);
        Band band = band(USER_ID);
        givenValidRequest(deadlineAt);
        when(bandMemberRepository.findById(BAND_MEMBER_ID))
                .thenReturn(Optional.of(BandMember.builder().band(band).build()));
        when(recruitmentRepository.save(any(SessionRecruitment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.createSessionRecruitment(USER_ID, request);

        ArgumentCaptor<SessionRecruitment> captor =
                ArgumentCaptor.forClass(SessionRecruitment.class);
        verify(recruitmentRepository).save(captor.capture());
        assertThat(captor.getValue().getBand()).isSameAs(band);
        assertThat(captor.getValue().getRecruitmentTitle()).isEqualTo("기타 세션 모집");
        assertThat(captor.getValue().getDeadlineAt()).isEqualTo(deadlineAt);
        assertThat(response.getBandId()).isEqualTo(band.getId());
        assertThat(response.getRecruitmentTitle()).isEqualTo("기타 세션 모집");
    }

    @Test
    @DisplayName("존재하지 않는 밴드 멤버 프로필로 모집 공고를 등록하면 실패한다")
    void createFailsWhenBandMemberDoesNotExist() {
        when(request.getBandMemberId()).thenReturn(BAND_MEMBER_ID);
        when(bandMemberRepository.findById(BAND_MEMBER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createSessionRecruitment(USER_ID, request))
                .isInstanceOf(BandException.class);

        verify(recruitmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("밴드 소유자가 아닌 사용자가 모집 공고를 등록하면 실패한다")
    void createFailsWhenUserIsNotBandOwner() {
        when(request.getBandMemberId()).thenReturn(BAND_MEMBER_ID);
        when(bandMemberRepository.findById(BAND_MEMBER_ID))
                .thenReturn(Optional.of(BandMember.builder().band(band(999L)).build()));

        assertThatThrownBy(() -> service.createSessionRecruitment(USER_ID, request))
                .isInstanceOf(SessionException.class)
                .extracting("baseResponseCode")
                .isEqualTo(SessionErrorCode.BAND_PERMISSION_DENIED);

        verify(recruitmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("마감일이 현재보다 이전이면 모집 공고 등록에 실패한다")
    void createFailsWhenDeadlineIsNotFuture() {
        when(request.getBandMemberId()).thenReturn(BAND_MEMBER_ID);
        when(request.getDeadlineAt()).thenReturn(LocalDateTime.now().minusSeconds(1));
        when(bandMemberRepository.findById(BAND_MEMBER_ID))
                .thenReturn(Optional.of(BandMember.builder().band(band(USER_ID)).build()));

        assertThatThrownBy(() -> service.createSessionRecruitment(USER_ID, request))
                .isInstanceOf(SessionException.class)
                .extracting("baseResponseCode")
                .isEqualTo(SessionErrorCode.INVALID_SESSION_RECRUITMENT_DEADLINE);

        verify(recruitmentRepository, never()).save(any());
    }

    private void givenValidRequest(LocalDateTime deadlineAt) {
        when(request.getBandMemberId()).thenReturn(BAND_MEMBER_ID);
        when(request.getRecruitmentTitle()).thenReturn("기타 세션 모집");
        when(request.getSummary()).thenReturn("주 1회 합주");
        when(request.getContent()).thenReturn("함께 공연할 기타 세션을 모집합니다.");
        when(request.getPart()).thenReturn(Part.GUITAR);
        when(request.getSkillLevel()).thenReturn(SkillLevel.INTERMEDIATE);
        when(request.getGenre()).thenReturn(Genre.HARD_ROCK);
        when(request.getRegion()).thenReturn(Region.SEOUL);
        when(request.getPracticeSchedule()).thenReturn("매주 토요일");
        when(request.getPracticePlace()).thenReturn("서울");
        when(request.getDeadlineAt()).thenReturn(deadlineAt);
        when(request.getQualification()).thenReturn("합주 경험");
    }

    private Band band(Long ownerId) {
        return Band.builder()
                .id(20L)
                .owner(User.builder().id(ownerId).name("소유자").build())
                .name("테스트 밴드")
                .genre(Genre.HARD_ROCK)
                .region(Region.SEOUL)
                .build();
    }
}
