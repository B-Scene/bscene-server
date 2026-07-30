package com.umc.bscene.domain.performance.service;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.performance.dto.response.PendingParticipationResponse;
import com.umc.bscene.domain.performance.dto.response.PerformanceAlarmResponse;
import com.umc.bscene.domain.performance.dto.response.PerformanceParticipationDeclineResponse;
import com.umc.bscene.domain.performance.dto.response.PerformanceParticipationResponse;
import com.umc.bscene.domain.performance.entity.Performance;
import com.umc.bscene.domain.performance.entity.PerformanceParticipation;
import com.umc.bscene.domain.performance.enums.AgeRating;
import com.umc.bscene.domain.performance.enums.ParticipationStatus;
import com.umc.bscene.domain.performance.enums.PerformanceStatus;
import com.umc.bscene.domain.performance.exception.PerformanceException;
import com.umc.bscene.domain.performance.repository.PerformanceParticipationRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 공연 알림 설정/해제 + 참여 확인(대기 목록/완료/불참) 단위테스트.
@ExtendWith(MockitoExtension.class)
class PerformanceAlarmServiceTest {

    @Mock
    private PerformanceParticipationRepository performanceParticipationRepository;
    @Mock
    private PerformanceRepository performanceRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PerformanceInterestService performanceInterestService;

    private PerformanceAlarmService service;

    private static final Long USER_ID = 1L;
    private static final Long BAND_ID = 10L;
    private static final Long PERFORMANCE_ID = 100L;

    @BeforeEach
    void setUp() {
        service = new PerformanceAlarmService(
                performanceParticipationRepository, performanceRepository, userRepository, performanceInterestService
        );
    }

    private User user() {
        return User.builder().id(USER_ID).build();
    }

    private Band band() {
        return Band.builder().id(BAND_ID).name("밴드").genre(Genre.HARD_ROCK).region(Region.SEOUL).build();
    }

    // 시작 일시와 상태를 지정한 공연 픽스처 (알림/참여 분기가 전부 시작 일시·상태에 걸려 있음)
    private Performance performance(LocalDate date, LocalTime time, PerformanceStatus status) {
        return Performance.builder()
                .id(PERFORMANCE_ID)
                .band(band())
                .title("공연")
                .genre(Genre.HARD_ROCK)
                .performanceDate(date)
                .startTime(time)
                .region(Region.SEOUL)
                .venue("홍대")
                .description("설명")
                .ticketPrice("10000")
                .ageRating(AgeRating.ALL)
                .status(status)
                .build();
    }

    private Performance futurePerformance() {
        return performance(LocalDate.now().plusDays(7), LocalTime.of(19, 0), PerformanceStatus.ACTIVE);
    }

    private Performance startedPerformance() {
        return performance(LocalDate.now().minusDays(1), LocalTime.of(19, 0), PerformanceStatus.ACTIVE);
    }

    private PerformanceParticipation participation(Performance performance, ParticipationStatus status) {
        return PerformanceParticipation.builder()
                .id(1000L)
                .performance(performance)
                .user(user())
                .status(status)
                .build();
    }

    // ---------- setAlarm ----------

    @Test
    void setAlarm_공연이_없으면_예외() {
        when(performanceRepository.findById(PERFORMANCE_ID)).thenReturn(Optional.empty());

        PerformanceException exception =
                assertThrows(PerformanceException.class, () -> service.setAlarm(USER_ID, PERFORMANCE_ID));

        assertEquals(PerformanceErrorCode.PERFORMANCE_NOT_FOUND, exception.getBaseResponseCode());
    }

    @Test
    void setAlarm_삭제된_공연이면_예외() {
        Performance deleted = performance(LocalDate.now().plusDays(7), LocalTime.of(19, 0), PerformanceStatus.DELETED);
        when(performanceRepository.findById(PERFORMANCE_ID)).thenReturn(Optional.of(deleted));

        PerformanceException exception =
                assertThrows(PerformanceException.class, () -> service.setAlarm(USER_ID, PERFORMANCE_ID));

        assertEquals(PerformanceErrorCode.PERFORMANCE_NOT_FOUND, exception.getBaseResponseCode());
    }

    @Test
    void setAlarm_이미_시작된_공연이면_예외() {
        when(performanceRepository.findById(PERFORMANCE_ID)).thenReturn(Optional.of(startedPerformance()));

        PerformanceException exception =
                assertThrows(PerformanceException.class, () -> service.setAlarm(USER_ID, PERFORMANCE_ID));

        assertEquals(PerformanceErrorCode.ALREADY_STARTED_PERFORMANCE, exception.getBaseResponseCode());
        verify(performanceParticipationRepository, never()).save(any());
    }

    @Test
    void setAlarm_이미_참여기록이_있으면_예외() {
        when(performanceRepository.findById(PERFORMANCE_ID)).thenReturn(Optional.of(futurePerformance()));
        when(performanceParticipationRepository.existsByPerformance_IdAndUser_Id(PERFORMANCE_ID, USER_ID))
                .thenReturn(true);

        PerformanceException exception =
                assertThrows(PerformanceException.class, () -> service.setAlarm(USER_ID, PERFORMANCE_ID));

        assertEquals(PerformanceErrorCode.ALREADY_ALARM_SET, exception.getBaseResponseCode());
        verify(performanceParticipationRepository, never()).save(any());
    }

    @Test
    void setAlarm_동시_요청으로_unique_위반이_나면_409로_변환한다() {
        when(performanceRepository.findById(PERFORMANCE_ID)).thenReturn(Optional.of(futurePerformance()));
        when(performanceParticipationRepository.existsByPerformance_IdAndUser_Id(PERFORMANCE_ID, USER_ID))
                .thenReturn(false);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user());
        when(performanceParticipationRepository.save(any(PerformanceParticipation.class)))
                .thenThrow(new DataIntegrityViolationException("unique 위반"));

        PerformanceException exception =
                assertThrows(PerformanceException.class, () -> service.setAlarm(USER_ID, PERFORMANCE_ID));

        assertEquals(PerformanceErrorCode.ALREADY_ALARM_SET, exception.getBaseResponseCode());
    }

    @Test
    void setAlarm_성공시_SCHEDULED_기록을_저장하고_관심공연도_함께_등록한다() {
        Performance performance = futurePerformance();
        when(performanceRepository.findById(PERFORMANCE_ID)).thenReturn(Optional.of(performance));
        when(performanceParticipationRepository.existsByPerformance_IdAndUser_Id(PERFORMANCE_ID, USER_ID))
                .thenReturn(false);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user());

        PerformanceAlarmResponse response = service.setAlarm(USER_ID, PERFORMANCE_ID);

        assertEquals(PERFORMANCE_ID, response.performanceId());
        assertTrue(response.isAlarmSet());

        ArgumentCaptor<PerformanceParticipation> captor = ArgumentCaptor.captor();
        verify(performanceParticipationRepository).save(captor.capture());
        assertEquals(ParticipationStatus.SCHEDULED, captor.getValue().getStatus());
        verify(performanceInterestService).ensureInterest(USER_ID, performance);
    }

    // ---------- unsetAlarm ----------

    @Test
    void unsetAlarm_공연이_없으면_예외() {
        when(performanceRepository.findById(PERFORMANCE_ID)).thenReturn(Optional.empty());

        PerformanceException exception =
                assertThrows(PerformanceException.class, () -> service.unsetAlarm(USER_ID, PERFORMANCE_ID));

        assertEquals(PerformanceErrorCode.PERFORMANCE_NOT_FOUND, exception.getBaseResponseCode());
    }

    @Test
    void unsetAlarm_SCHEDULED_기록만_삭제하고_멱등하게_성공한다() {
        when(performanceRepository.findById(PERFORMANCE_ID)).thenReturn(Optional.of(futurePerformance()));

        PerformanceAlarmResponse response = service.unsetAlarm(USER_ID, PERFORMANCE_ID);

        assertEquals(PERFORMANCE_ID, response.performanceId());
        assertFalse(response.isAlarmSet());
        // 참여완료(COMPLETED) 이력은 지우지 않도록 SCHEDULED 조건으로만 삭제
        verify(performanceParticipationRepository).deleteByPerformance_IdAndUser_IdAndStatus(
                PERFORMANCE_ID, USER_ID, ParticipationStatus.SCHEDULED);
    }

    // ---------- getPendingParticipations ----------

    @Test
    void getPendingParticipations_시작시간이_지난_SCHEDULED_공연을_반환한다() {
        Performance performance = startedPerformance();
        when(performanceParticipationRepository.findPendingConfirmations(
                eq(USER_ID), eq(ParticipationStatus.SCHEDULED), eq(PerformanceStatus.ACTIVE), any(), any()))
                .thenReturn(List.of(participation(performance, ParticipationStatus.SCHEDULED)));

        PendingParticipationResponse response = service.getPendingParticipations(USER_ID);

        assertEquals(1, response.items().size());
        assertEquals(PERFORMANCE_ID, response.items().get(0).performanceId());
        assertEquals("공연", response.items().get(0).title());
    }

    // ---------- completeParticipation ----------

    @Test
    void completeParticipation_참여기록이_없으면_예외() {
        when(performanceParticipationRepository.findByPerformance_IdAndUser_Id(PERFORMANCE_ID, USER_ID))
                .thenReturn(Optional.empty());

        PerformanceException exception =
                assertThrows(PerformanceException.class, () -> service.completeParticipation(USER_ID, PERFORMANCE_ID));

        assertEquals(PerformanceErrorCode.PARTICIPATION_NOT_FOUND, exception.getBaseResponseCode());
    }

    @Test
    void completeParticipation_삭제된_공연이면_예외() {
        Performance deleted = performance(LocalDate.now().minusDays(1), LocalTime.of(19, 0), PerformanceStatus.DELETED);
        when(performanceParticipationRepository.findByPerformance_IdAndUser_Id(PERFORMANCE_ID, USER_ID))
                .thenReturn(Optional.of(participation(deleted, ParticipationStatus.SCHEDULED)));

        PerformanceException exception =
                assertThrows(PerformanceException.class, () -> service.completeParticipation(USER_ID, PERFORMANCE_ID));

        assertEquals(PerformanceErrorCode.PERFORMANCE_NOT_FOUND, exception.getBaseResponseCode());
    }

    @Test
    void completeParticipation_시작_전_공연이면_예외() {
        PerformanceParticipation participation =
                participation(futurePerformance(), ParticipationStatus.SCHEDULED);
        when(performanceParticipationRepository.findByPerformance_IdAndUser_Id(PERFORMANCE_ID, USER_ID))
                .thenReturn(Optional.of(participation));

        PerformanceException exception =
                assertThrows(PerformanceException.class, () -> service.completeParticipation(USER_ID, PERFORMANCE_ID));

        assertEquals(PerformanceErrorCode.PERFORMANCE_NOT_STARTED, exception.getBaseResponseCode());
        assertEquals(ParticipationStatus.SCHEDULED, participation.getStatus());
    }

    @Test
    void completeParticipation_성공시_COMPLETED로_전이된다() {
        PerformanceParticipation participation =
                participation(startedPerformance(), ParticipationStatus.SCHEDULED);
        when(performanceParticipationRepository.findByPerformance_IdAndUser_Id(PERFORMANCE_ID, USER_ID))
                .thenReturn(Optional.of(participation));

        PerformanceParticipationResponse response = service.completeParticipation(USER_ID, PERFORMANCE_ID);

        assertEquals(ParticipationStatus.COMPLETED, participation.getStatus());
        assertEquals(ParticipationStatus.COMPLETED, response.status());
    }

    @Test
    void completeParticipation_이미_완료된_기록이면_멱등하게_COMPLETED를_유지한다() {
        PerformanceParticipation participation =
                participation(startedPerformance(), ParticipationStatus.COMPLETED);
        when(performanceParticipationRepository.findByPerformance_IdAndUser_Id(PERFORMANCE_ID, USER_ID))
                .thenReturn(Optional.of(participation));

        PerformanceParticipationResponse response = service.completeParticipation(USER_ID, PERFORMANCE_ID);

        assertEquals(ParticipationStatus.COMPLETED, response.status());
    }

    // ---------- declineParticipation ----------

    @Test
    void declineParticipation_참여기록이_없으면_예외() {
        when(performanceParticipationRepository.findByPerformance_IdAndUser_Id(PERFORMANCE_ID, USER_ID))
                .thenReturn(Optional.empty());

        PerformanceException exception =
                assertThrows(PerformanceException.class, () -> service.declineParticipation(USER_ID, PERFORMANCE_ID));

        assertEquals(PerformanceErrorCode.PARTICIPATION_NOT_FOUND, exception.getBaseResponseCode());
    }

    @Test
    void declineParticipation_이미_참여완료면_이력_보호를_위해_예외() {
        when(performanceParticipationRepository.findByPerformance_IdAndUser_Id(PERFORMANCE_ID, USER_ID))
                .thenReturn(Optional.of(participation(startedPerformance(), ParticipationStatus.COMPLETED)));

        PerformanceException exception =
                assertThrows(PerformanceException.class, () -> service.declineParticipation(USER_ID, PERFORMANCE_ID));

        assertEquals(PerformanceErrorCode.ALREADY_COMPLETED_PARTICIPATION, exception.getBaseResponseCode());
        verify(performanceParticipationRepository, never()).delete(any());
    }

    @Test
    void declineParticipation_성공시_SCHEDULED_기록을_삭제한다() {
        PerformanceParticipation participation =
                participation(startedPerformance(), ParticipationStatus.SCHEDULED);
        when(performanceParticipationRepository.findByPerformance_IdAndUser_Id(PERFORMANCE_ID, USER_ID))
                .thenReturn(Optional.of(participation));

        PerformanceParticipationDeclineResponse response = service.declineParticipation(USER_ID, PERFORMANCE_ID);

        assertEquals(PERFORMANCE_ID, response.performanceId());
        verify(performanceParticipationRepository).delete(participation);
    }
}
