package com.umc.bscene.domain.session.service;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.session.entity.SessionRecruitment;
import com.umc.bscene.domain.session.entity.SessionRecruitmentInterest;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.session.enums.SkillLevel;
import com.umc.bscene.domain.session.enums.code.error.SessionErrorCode;
import com.umc.bscene.domain.session.exception.SessionException;
import com.umc.bscene.domain.session.repository.SessionRecruitmentInterestRepository;
import com.umc.bscene.domain.session.repository.SessionRecruitmentRepository;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
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
class SessionRecruitmentInterestServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long RECRUITMENT_ID = 2L;

    @Mock
    private SessionRecruitmentInterestRepository interestRepository;
    @Mock
    private SessionRecruitmentRepository recruitmentRepository;
    @Mock
    private UserRepository userRepository;

    private SessionRecruitmentInterestService service;

    @BeforeEach
    void setUp() {
        service = new SessionRecruitmentInterestService(
                interestRepository,
                recruitmentRepository,
                userRepository
        );
    }

    @Test
    @DisplayName("세션 모집 공고를 관심 목록에 추가한다")
    void setInterestSuccess() {
        SessionRecruitment recruitment = recruitment();
        User user = User.builder().id(USER_ID).name("사용자").build();
        when(recruitmentRepository
                .findBySessionRecruitmentIdAndDeletedAtIsNull(RECRUITMENT_ID))
                .thenReturn(Optional.of(recruitment));
        when(interestRepository
                .existsBySessionRecruitment_SessionRecruitmentIdAndUser_Id(
                        RECRUITMENT_ID, USER_ID
                )).thenReturn(false);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user);

        var response = service.setInterest(USER_ID, RECRUITMENT_ID);

        var captor = ArgumentCaptor.forClass(
                com.umc.bscene.domain.session.entity.SessionRecruitmentInterest.class
        );
        verify(interestRepository).save(captor.capture());
        assertThat(captor.getValue().getSessionRecruitment()).isSameAs(recruitment);
        assertThat(captor.getValue().getUser()).isSameAs(user);
        assertThat(response.sessionRecruitmentId()).isEqualTo(RECRUITMENT_ID);
        assertThat(response.isInterested()).isTrue();
    }

    @Test
    @DisplayName("이미 관심 등록한 모집 공고를 다시 등록하면 실패한다")
    void setInterestFailsWhenAlreadyExists() {
        when(recruitmentRepository
                .findBySessionRecruitmentIdAndDeletedAtIsNull(RECRUITMENT_ID))
                .thenReturn(Optional.of(recruitment()));
        when(interestRepository
                .existsBySessionRecruitment_SessionRecruitmentIdAndUser_Id(
                        RECRUITMENT_ID, USER_ID
                )).thenReturn(true);

        assertThatThrownBy(() -> service.setInterest(USER_ID, RECRUITMENT_ID))
                .isInstanceOf(SessionException.class)
                .extracting("baseResponseCode")
                .isEqualTo(SessionErrorCode.SESSION_RECRUITMENT_INTEREST_ALREADY_EXISTS);

        verify(interestRepository, never()).save(any());
    }

    @Test
    @DisplayName("동시에 중복 관심 등록이 발생해도 도메인 예외로 변환한다")
    void setInterestConvertsDuplicateKeyException() {
        when(recruitmentRepository
                .findBySessionRecruitmentIdAndDeletedAtIsNull(RECRUITMENT_ID))
                .thenReturn(Optional.of(recruitment()));
        when(interestRepository
                .existsBySessionRecruitment_SessionRecruitmentIdAndUser_Id(
                        RECRUITMENT_ID, USER_ID
                )).thenReturn(false);
        when(userRepository.getReferenceById(USER_ID))
                .thenReturn(User.builder().id(USER_ID).build());
        when(interestRepository.save(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> service.setInterest(USER_ID, RECRUITMENT_ID))
                .isInstanceOf(SessionException.class)
                .extracting("baseResponseCode")
                .isEqualTo(SessionErrorCode.SESSION_RECRUITMENT_INTEREST_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("세션 모집 공고 관심 등록을 취소한다")
    void unsetInterestSuccess() {
        when(recruitmentRepository
                .findBySessionRecruitmentIdAndDeletedAtIsNull(RECRUITMENT_ID))
                .thenReturn(Optional.of(recruitment()));

        var response = service.unsetInterest(USER_ID, RECRUITMENT_ID);

        verify(interestRepository)
                .deleteBySessionRecruitment_SessionRecruitmentIdAndUser_Id(
                        RECRUITMENT_ID, USER_ID
                );
        assertThat(response.sessionRecruitmentId()).isEqualTo(RECRUITMENT_ID);
        assertThat(response.isInterested()).isFalse();
    }

    @Test
    @DisplayName("삭제되었거나 존재하지 않는 모집 공고의 관심 상태는 변경할 수 없다")
    void interestFailsWhenRecruitmentDoesNotExist() {
        when(recruitmentRepository
                .findBySessionRecruitmentIdAndDeletedAtIsNull(RECRUITMENT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.unsetInterest(USER_ID, RECRUITMENT_ID))
                .isInstanceOf(SessionException.class)
                .extracting("baseResponseCode")
                .isEqualTo(SessionErrorCode.SESSION_RECRUITMENT_NOT_FOUND);

        verify(interestRepository, never())
                .deleteBySessionRecruitment_SessionRecruitmentIdAndUser_Id(any(), any());
    }

    @Test
    @DisplayName("스크랩한 모집 공고를 커서 방식으로 조회한다")
    void getMyInterestsCalculatesNextCursor() {
        SessionRecruitmentInterest first = interest(10L, 20L);
        SessionRecruitmentInterest second = interest(9L, 19L);
        when(interestRepository.findMyInterests(
                eq(USER_ID), eq(null), any(Pageable.class)
        )).thenReturn(List.of(first, second));

        var response = service.getMyInterests(USER_ID, null, 1);

        assertThat(response.content()).hasSize(1);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.nextCursor()).isEqualTo(10L);
        assertThat(response.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("스크랩 조회 크기는 기본 10개이고 최대 50개다")
    void getMyInterestsUsesDefaultAndMaximumSize() {
        when(interestRepository.findMyInterests(
                eq(USER_ID), eq(null), any(Pageable.class)
        )).thenReturn(List.of());

        var defaultSize = service.getMyInterests(USER_ID, null, null);
        var maximumSize = service.getMyInterests(USER_ID, null, 100);

        assertThat(defaultSize.size()).isEqualTo(10);
        assertThat(maximumSize.size()).isEqualTo(50);
    }

    private SessionRecruitment recruitment() {
        return SessionRecruitment.builder()
                .sessionRecruitmentId(RECRUITMENT_ID)
                .build();
    }

    private SessionRecruitmentInterest interest(Long interestId, Long recruitmentId) {
        User owner = User.builder().id(2L).build();
        Band band = Band.builder()
                .id(3L)
                .owner(owner)
                .name("테스트 밴드")
                .genre(Genre.HARD_ROCK)
                .region(Region.SEOUL)
                .build();
        SessionRecruitment recruitment = SessionRecruitment.builder()
                .sessionRecruitmentId(recruitmentId)
                .band(band)
                .recruitmentTitle("기타 모집")
                .summary("주 1회 합주")
                .part(Part.GUITAR)
                .skillLevel(SkillLevel.INTERMEDIATE)
                .genre(Genre.HARD_ROCK)
                .region(Region.SEOUL)
                .deadlineAt(LocalDateTime.now().plusDays(7))
                .build();
        ReflectionTestUtils.setField(
                recruitment, "createdAt", LocalDateTime.now().minusDays(1)
        );
        SessionRecruitmentInterest interest =
                SessionRecruitmentInterest.builder()
                        .sessionRecruitment(recruitment)
                        .user(User.builder().id(USER_ID).build())
                        .build();
        ReflectionTestUtils.setField(
                interest, "sessionRecruitmentInterestId", interestId
        );
        return interest;
    }
}
