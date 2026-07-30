package com.umc.bscene.domain.performance.service;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.performance.dto.response.PerformanceInterestResponse;
import com.umc.bscene.domain.performance.entity.Performance;
import com.umc.bscene.domain.performance.entity.PerformanceInterest;
import com.umc.bscene.domain.performance.enums.AgeRating;
import com.umc.bscene.domain.performance.enums.PerformanceStatus;
import com.umc.bscene.domain.performance.exception.PerformanceException;
import com.umc.bscene.domain.performance.repository.PerformanceInterestRepository;
import com.umc.bscene.domain.performance.repository.PerformanceRepository;
import com.umc.bscene.domain.performance.response.code.PerformanceErrorCode;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 관심 공연 등록/해제 단위테스트.
@ExtendWith(MockitoExtension.class)
class PerformanceInterestServiceTest {

    @Mock
    private PerformanceInterestRepository performanceInterestRepository;
    @Mock
    private PerformanceRepository performanceRepository;
    @Mock
    private UserRepository userRepository;

    private PerformanceInterestService service;

    private static final Long USER_ID = 1L;
    private static final Long BAND_ID = 10L;
    private static final Long PERFORMANCE_ID = 100L;

    @BeforeEach
    void setUp() {
        service = new PerformanceInterestService(
                performanceInterestRepository, performanceRepository, userRepository
        );
    }

    private User user() {
        return User.builder().id(USER_ID).build();
    }

    private Performance performance(PerformanceStatus status) {
        return Performance.builder()
                .id(PERFORMANCE_ID)
                .band(Band.builder().id(BAND_ID).name("밴드").genre(Genre.HARD_ROCK).region(Region.SEOUL).build())
                .title("공연")
                .genre(Genre.HARD_ROCK)
                .performanceDate(LocalDate.now().plusDays(7))
                .startTime(LocalTime.of(19, 0))
                .region(Region.SEOUL)
                .venue("홍대")
                .description("설명")
                .ticketPrice("10000")
                .ageRating(AgeRating.ALL)
                .status(status)
                .build();
    }

    // ---------- setInterest ----------

    @Test
    void setInterest_공연이_없으면_예외() {
        when(performanceRepository.findById(PERFORMANCE_ID)).thenReturn(Optional.empty());

        PerformanceException exception =
                assertThrows(PerformanceException.class, () -> service.setInterest(USER_ID, PERFORMANCE_ID));

        assertEquals(PerformanceErrorCode.PERFORMANCE_NOT_FOUND, exception.getBaseResponseCode());
    }

    @Test
    void setInterest_삭제된_공연이면_예외() {
        when(performanceRepository.findById(PERFORMANCE_ID))
                .thenReturn(Optional.of(performance(PerformanceStatus.DELETED)));

        PerformanceException exception =
                assertThrows(PerformanceException.class, () -> service.setInterest(USER_ID, PERFORMANCE_ID));

        assertEquals(PerformanceErrorCode.PERFORMANCE_NOT_FOUND, exception.getBaseResponseCode());
    }

    @Test
    void setInterest_이미_관심_등록했으면_예외() {
        when(performanceRepository.findById(PERFORMANCE_ID))
                .thenReturn(Optional.of(performance(PerformanceStatus.ACTIVE)));
        when(performanceInterestRepository.existsByPerformance_IdAndUser_Id(PERFORMANCE_ID, USER_ID))
                .thenReturn(true);

        PerformanceException exception =
                assertThrows(PerformanceException.class, () -> service.setInterest(USER_ID, PERFORMANCE_ID));

        assertEquals(PerformanceErrorCode.ALREADY_INTEREST_SET, exception.getBaseResponseCode());
        verify(performanceInterestRepository, never()).save(any());
    }

    @Test
    void setInterest_동시_요청으로_unique_위반이_나면_409로_변환한다() {
        when(performanceRepository.findById(PERFORMANCE_ID))
                .thenReturn(Optional.of(performance(PerformanceStatus.ACTIVE)));
        when(performanceInterestRepository.existsByPerformance_IdAndUser_Id(PERFORMANCE_ID, USER_ID))
                .thenReturn(false);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user());
        when(performanceInterestRepository.save(any(PerformanceInterest.class)))
                .thenThrow(new DataIntegrityViolationException("unique 위반"));

        PerformanceException exception =
                assertThrows(PerformanceException.class, () -> service.setInterest(USER_ID, PERFORMANCE_ID));

        assertEquals(PerformanceErrorCode.ALREADY_INTEREST_SET, exception.getBaseResponseCode());
    }

    @Test
    void setInterest_성공시_관심_등록을_저장하고_true를_반환한다() {
        when(performanceRepository.findById(PERFORMANCE_ID))
                .thenReturn(Optional.of(performance(PerformanceStatus.ACTIVE)));
        when(performanceInterestRepository.existsByPerformance_IdAndUser_Id(PERFORMANCE_ID, USER_ID))
                .thenReturn(false);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user());

        PerformanceInterestResponse response = service.setInterest(USER_ID, PERFORMANCE_ID);

        assertEquals(PERFORMANCE_ID, response.performanceId());
        assertTrue(response.isInterested());

        ArgumentCaptor<PerformanceInterest> captor = ArgumentCaptor.captor();
        verify(performanceInterestRepository).save(captor.capture());
        assertEquals(PERFORMANCE_ID, captor.getValue().getPerformance().getId());
    }

    // ---------- ensureInterest (알림 설정에서 호출되는 멱등 등록) ----------

    @Test
    void ensureInterest_이미_등록돼_있으면_저장하지_않고_통과한다() {
        when(performanceInterestRepository.existsByPerformance_IdAndUser_Id(PERFORMANCE_ID, USER_ID))
                .thenReturn(true);

        service.ensureInterest(USER_ID, performance(PerformanceStatus.ACTIVE));

        verify(performanceInterestRepository, never()).save(any());
    }

    @Test
    void ensureInterest_등록이_없으면_새로_저장한다() {
        when(performanceInterestRepository.existsByPerformance_IdAndUser_Id(PERFORMANCE_ID, USER_ID))
                .thenReturn(false);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user());

        service.ensureInterest(USER_ID, performance(PerformanceStatus.ACTIVE));

        verify(performanceInterestRepository).save(any(PerformanceInterest.class));
    }

    // ---------- unsetInterest ----------

    @Test
    void unsetInterest_등록_여부와_관계없이_멱등하게_해제한다() {
        PerformanceInterestResponse response = service.unsetInterest(USER_ID, PERFORMANCE_ID);

        assertEquals(PERFORMANCE_ID, response.performanceId());
        assertFalse(response.isInterested());
        verify(performanceInterestRepository).deleteByPerformance_IdAndUser_Id(PERFORMANCE_ID, USER_ID);
    }
}
