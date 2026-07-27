package com.umc.bscene.domain.performance.service;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.exception.BandException;
import com.umc.bscene.domain.band.repository.BandMemberRepository;
import com.umc.bscene.domain.band.repository.BandRepository;
import com.umc.bscene.domain.band.response.code.BandErrorCode;
import com.umc.bscene.domain.performance.dto.request.PerformanceCreateRequest;
import com.umc.bscene.domain.performance.dto.request.PerformanceUpdateRequest;
import com.umc.bscene.domain.performance.dto.response.PerformanceDetailResponse;
import com.umc.bscene.domain.performance.dto.response.PerformanceListResponse;
import com.umc.bscene.domain.performance.dto.response.PerformanceResponse;
import com.umc.bscene.domain.performance.entity.Performance;
import com.umc.bscene.domain.performance.entity.PerformanceParticipation;
import com.umc.bscene.domain.performance.enums.AgeRating;
import com.umc.bscene.domain.performance.enums.ParticipationStatus;
import com.umc.bscene.domain.performance.enums.PerformanceStatus;
import com.umc.bscene.domain.performance.exception.PerformanceException;
import com.umc.bscene.domain.performance.port.FollowPort;
import com.umc.bscene.domain.performance.port.NotifyPort;
import com.umc.bscene.domain.performance.repository.PerformanceInterestRepository;
import com.umc.bscene.domain.performance.repository.PerformanceParticipationRepository;
import com.umc.bscene.domain.performance.repository.PerformanceRepository;
import com.umc.bscene.domain.performance.response.code.PerformanceErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerformanceServiceTest {

    @Mock
    private PerformanceRepository performanceRepository;
    @Mock
    private PerformanceInterestRepository performanceInterestRepository;
    @Mock
    private BandRepository bandRepository;
    @Mock
    private BandMemberRepository bandMemberRepository;
    @Mock
    private FollowPort followPort;
    @Mock
    private NotifyPort notifyPort;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private PerformanceParticipationRepository performanceParticipationRepository;

    private PerformanceService service;

    private static final Long USER_ID = 1L;
    private static final Long BAND_ID = 10L;
    private static final Long PERFORMANCE_ID = 100L;

    @BeforeEach
    void setUp() {
        service = new PerformanceService(
                performanceRepository, performanceInterestRepository, bandRepository, bandMemberRepository,
                followPort, notifyPort, eventPublisher, performanceParticipationRepository
        );
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    private Band band() {
        return Band.builder().id(BAND_ID).name("밴드").genre(Genre.HARD_ROCK).region(Region.SEOUL).build();
    }

    private Performance performance(LocalDate date) {
        return Performance.builder()
                .id(PERFORMANCE_ID)
                .band(band())
                .title("공연")
                .genre(Genre.HARD_ROCK)
                .performanceDate(date)
                .startTime(LocalTime.of(19, 0))
                .region(Region.SEOUL)
                .venue("홍대")
                .description("설명")
                .ticketPrice("10000")
                .ageRating(AgeRating.ALL)
                .build();
    }

    private PerformanceCreateRequest createRequest(LocalDate date, List<String> tags) {
        return new PerformanceCreateRequest(
                "공연", Genre.HARD_ROCK, date, LocalTime.of(19, 0), Region.SEOUL,
                "홍대", "설명", "10000", null, null, AgeRating.ALL, tags
        );
    }

    // ---------- createPerformance ----------

    @Test
    void createPerformance_밴드가_없으면_예외() {
        when(bandRepository.findById(BAND_ID)).thenReturn(Optional.empty());
        PerformanceCreateRequest request = createRequest(LocalDate.now().plusDays(1), List.of());

        BandException exception = assertThrows(BandException.class,
                () -> service.createPerformance(USER_ID, BAND_ID, request));

        assertEquals(BandErrorCode.BAND_NOT_FOUND, exception.getBaseResponseCode());
    }

    @Test
    void createPerformance_밴드멤버가_아니면_예외() {
        when(bandRepository.findById(BAND_ID)).thenReturn(Optional.of(band()));
        when(bandMemberRepository.existsByBand_IdAndUser_IdAndStatus(eq(BAND_ID), eq(USER_ID), any())).thenReturn(false);
        PerformanceCreateRequest request = createRequest(LocalDate.now().plusDays(1), List.of());

        PerformanceException exception = assertThrows(PerformanceException.class,
                () -> service.createPerformance(USER_ID, BAND_ID, request));

        assertEquals(PerformanceErrorCode.NOT_PERFORMANCE_BAND_MEMBER, exception.getBaseResponseCode());
    }

    @Test
    void createPerformance_지난날짜면_예외() {
        when(bandRepository.findById(BAND_ID)).thenReturn(Optional.of(band()));
        when(bandMemberRepository.existsByBand_IdAndUser_IdAndStatus(eq(BAND_ID), eq(USER_ID), any())).thenReturn(true);
        PerformanceCreateRequest request = createRequest(LocalDate.now().minusDays(1), List.of());

        PerformanceException exception = assertThrows(PerformanceException.class,
                () -> service.createPerformance(USER_ID, BAND_ID, request));

        assertEquals(PerformanceErrorCode.PAST_DATE_NOT_ALLOWED, exception.getBaseResponseCode());
    }

    @Test
    void createPerformance_태그가_8개초과면_예외() {
        when(bandRepository.findById(BAND_ID)).thenReturn(Optional.of(band()));
        when(bandMemberRepository.existsByBand_IdAndUser_IdAndStatus(eq(BAND_ID), eq(USER_ID), any())).thenReturn(true);
        List<String> tooManyTags = List.of("1", "2", "3", "4", "5", "6", "7", "8", "9");
        PerformanceCreateRequest request = createRequest(LocalDate.now().plusDays(1), tooManyTags);

        PerformanceException exception = assertThrows(PerformanceException.class,
                () -> service.createPerformance(USER_ID, BAND_ID, request));

        assertEquals(PerformanceErrorCode.TAG_LIMIT_EXCEEDED, exception.getBaseResponseCode());
    }

    @Test
    void createPerformance_성공시_등록한_공연을_반환한다() {
        when(bandRepository.findById(BAND_ID)).thenReturn(Optional.of(band()));
        when(bandMemberRepository.existsByBand_IdAndUser_IdAndStatus(eq(BAND_ID), eq(USER_ID), any())).thenReturn(true);
        when(performanceRepository.save(any(Performance.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(followPort.getFollowerUserIdsByBandId(BAND_ID)).thenReturn(List.of());
        PerformanceCreateRequest request = createRequest(LocalDate.now().plusDays(1), List.of("락"));

        PerformanceResponse response = service.createPerformance(USER_ID, BAND_ID, request);

        assertEquals("공연", response.title());
        assertEquals(List.of("락"), response.tags());
    }

    // ---------- getPerformances ----------

    @Test
    void getPerformances_밴드의_ACTIVE_공연만_조회한다() {
        when(bandRepository.findById(BAND_ID)).thenReturn(Optional.of(band()));
        when(performanceRepository.findByBand_IdAndStatusOrderByPerformanceDateAsc(BAND_ID, PerformanceStatus.ACTIVE))
                .thenReturn(List.of());

        PerformanceListResponse response = service.getPerformances(BAND_ID);

        assertEquals(0, response.performances().size());
    }

    // ---------- getPerformanceDetail ----------

    @Test
    void getPerformanceDetail_삭제된_공연이면_예외() {
        Performance deleted = Performance.builder()
                .id(PERFORMANCE_ID).band(band()).title("공연").genre(Genre.HARD_ROCK)
                .performanceDate(LocalDate.now()).startTime(LocalTime.NOON).region(Region.SEOUL)
                .venue("홍대").description("설명").ticketPrice("10000").ageRating(AgeRating.ALL)
                .status(PerformanceStatus.DELETED).build();
        when(performanceRepository.findById(PERFORMANCE_ID)).thenReturn(Optional.of(deleted));

        PerformanceException exception = assertThrows(PerformanceException.class,
                () -> service.getPerformanceDetail(USER_ID, PERFORMANCE_ID));

        assertEquals(PerformanceErrorCode.PERFORMANCE_NOT_FOUND, exception.getBaseResponseCode());
    }

    @Test
    void getPerformanceDetail_관심수와_관심여부를_포함해_반환한다() {
        when(performanceRepository.findById(PERFORMANCE_ID)).thenReturn(Optional.of(performance(LocalDate.now())));
        when(performanceInterestRepository.countByPerformance_Id(PERFORMANCE_ID)).thenReturn(3L);
        when(performanceInterestRepository.existsByPerformance_IdAndUser_Id(PERFORMANCE_ID, USER_ID)).thenReturn(true);

        PerformanceResponse response = service.getPerformanceDetail(USER_ID, PERFORMANCE_ID);

        assertEquals(3L, response.interestCount());
        assertEquals(true, response.isInterested());
    }

    // ---------- getPerformanceDetailPage ----------

    @Test
    void getPerformanceDetailPage_참여기록이_없으면_participationStatus는_null() {
        when(performanceRepository.findById(PERFORMANCE_ID)).thenReturn(Optional.of(performance(LocalDate.now())));
        when(performanceInterestRepository.countByPerformance_Id(PERFORMANCE_ID)).thenReturn(0L);
        when(performanceInterestRepository.existsByPerformance_IdAndUser_Id(PERFORMANCE_ID, USER_ID)).thenReturn(false);
        when(performanceParticipationRepository.findByPerformance_IdAndUser_Id(PERFORMANCE_ID, USER_ID))
                .thenReturn(Optional.empty());

        PerformanceDetailResponse response = service.getPerformanceDetailPage(USER_ID, PERFORMANCE_ID);

        assertNull(response.participationStatus());
    }

    @Test
    void getPerformanceDetailPage_참여기록이_있으면_상태를_반환한다() {
        Performance performance = performance(LocalDate.now());
        when(performanceRepository.findById(PERFORMANCE_ID)).thenReturn(Optional.of(performance));
        when(performanceInterestRepository.countByPerformance_Id(PERFORMANCE_ID)).thenReturn(0L);
        when(performanceInterestRepository.existsByPerformance_IdAndUser_Id(PERFORMANCE_ID, USER_ID)).thenReturn(false);
        PerformanceParticipation participation = PerformanceParticipation.builder()
                .performance(performance).build();
        when(performanceParticipationRepository.findByPerformance_IdAndUser_Id(PERFORMANCE_ID, USER_ID))
                .thenReturn(Optional.of(participation));

        PerformanceDetailResponse response = service.getPerformanceDetailPage(USER_ID, PERFORMANCE_ID);

        assertEquals(ParticipationStatus.SCHEDULED.name(), response.participationStatus());
    }

    // ---------- updatePerformance ----------

    @Test
    void updatePerformance_밴드멤버가_아니면_예외() {
        when(performanceRepository.findById(PERFORMANCE_ID)).thenReturn(Optional.of(performance(LocalDate.now())));
        when(bandMemberRepository.existsByBand_IdAndUser_IdAndStatus(eq(BAND_ID), eq(USER_ID), any())).thenReturn(false);
        PerformanceUpdateRequest request = new PerformanceUpdateRequest(
                "새제목", null, null, null, null, null, null, null, null, null
        );

        PerformanceException exception = assertThrows(PerformanceException.class,
                () -> service.updatePerformance(USER_ID, PERFORMANCE_ID, request));

        assertEquals(PerformanceErrorCode.NOT_PERFORMANCE_BAND_MEMBER, exception.getBaseResponseCode());
    }

    @Test
    void updatePerformance_변경할_날짜가_과거면_예외() {
        when(performanceRepository.findById(PERFORMANCE_ID)).thenReturn(Optional.of(performance(LocalDate.now())));
        when(bandMemberRepository.existsByBand_IdAndUser_IdAndStatus(eq(BAND_ID), eq(USER_ID), any())).thenReturn(true);
        PerformanceUpdateRequest request = new PerformanceUpdateRequest(
                null, null, LocalDate.now().minusDays(1), null, null, null, null, null, null, null
        );

        PerformanceException exception = assertThrows(PerformanceException.class,
                () -> service.updatePerformance(USER_ID, PERFORMANCE_ID, request));

        assertEquals(PerformanceErrorCode.PAST_DATE_NOT_ALLOWED, exception.getBaseResponseCode());
    }

    @Test
    void updatePerformance_성공시_제목이_수정된다() {
        Performance performance = performance(LocalDate.now().plusDays(1));
        when(performanceRepository.findById(PERFORMANCE_ID)).thenReturn(Optional.of(performance));
        when(bandMemberRepository.existsByBand_IdAndUser_IdAndStatus(eq(BAND_ID), eq(USER_ID), any())).thenReturn(true);
        when(performanceInterestRepository.countByPerformance_Id(PERFORMANCE_ID)).thenReturn(0L);
        when(performanceInterestRepository.existsByPerformance_IdAndUser_Id(PERFORMANCE_ID, USER_ID)).thenReturn(false);
        when(performanceParticipationRepository.findUserIdsByPerformanceIdAndStatus(PERFORMANCE_ID, ParticipationStatus.SCHEDULED))
                .thenReturn(List.of());
        PerformanceUpdateRequest request = new PerformanceUpdateRequest(
                "새제목", null, null, null, null, null, null, null, null, null
        );

        PerformanceResponse response = service.updatePerformance(USER_ID, PERFORMANCE_ID, request);

        assertEquals("새제목", response.title());
    }

    // ---------- deletePerformance ----------

    @Test
    void deletePerformance_성공시_상태가_DELETED로_변경된다() {
        Performance performance = performance(LocalDate.now());
        when(performanceRepository.findById(PERFORMANCE_ID)).thenReturn(Optional.of(performance));
        when(bandMemberRepository.existsByBand_IdAndUser_IdAndStatus(eq(BAND_ID), eq(USER_ID), any())).thenReturn(true);

        service.deletePerformance(USER_ID, PERFORMANCE_ID);

        assertEquals(PerformanceStatus.DELETED, performance.getStatus());
    }
}
