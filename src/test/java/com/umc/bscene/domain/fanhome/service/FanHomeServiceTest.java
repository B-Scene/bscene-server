package com.umc.bscene.domain.fanhome.service;

import com.umc.bscene.domain.fanhome.dto.response.FanHomeResponse;
import com.umc.bscene.domain.fanhome.dto.response.FanHomeResponse.HomePerformanceItem;
import com.umc.bscene.domain.fanhome.dto.response.FanHomeResponse.RecommendedBandItem;
import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.fanhome.enums.PerformanceSectionType;
import com.umc.bscene.domain.fanhome.enums.UpcomingSortType;
import com.umc.bscene.domain.fanhome.port.BandPort;
import com.umc.bscene.domain.fanhome.port.FollowPort;
import com.umc.bscene.domain.fanhome.port.NotificationPort;
import com.umc.bscene.domain.fanhome.port.PerformancePort;
import com.umc.bscene.domain.fanhome.port.PostPort;
import com.umc.bscene.global.exception.BaseException;
import com.umc.bscene.global.response.code.GeneralErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 팬모드 홈 (통합 조회 / 소식 전체 / 다가오는 공연 전체 / 공연 달력 / 날짜별 공연) 단위테스트.
@ExtendWith(MockitoExtension.class)
class FanHomeServiceTest {

    @Mock
    private FollowPort followPort;
    @Mock
    private PostPort postPort;
    @Mock
    private BandPort bandPort;
    @Mock
    private PerformancePort performancePort;
    @Mock
    private NotificationPort notificationPort;

    private FanHomeService service;

    private static final Long USER_ID = 1L;
    private static final List<Long> FOLLOWING_BAND_IDS = List.of(10L, 11L);

    @BeforeEach
    void setUp() {
        service = new FanHomeService(followPort, postPort, bandPort, performancePort, notificationPort);
    }

    private HomePerformanceItem performanceItem(Long id) {
        return new HomePerformanceItem(id, "공연", "홍대", LocalDate.now().plusDays(3), LocalTime.of(19, 0), null);
    }

    private RecommendedBandItem recommendedBand(Long id) {
        return new RecommendedBandItem(id, "밴드", Genre.INDIE, Region.SEOUL, null);
    }

    // ---------- getFanHome ----------

    @Test
    void getFanHome_팔로우한_밴드가_없으면_추천_밴드와_추천_공연을_반환한다() {
        when(followPort.findFollowingBandIds(USER_ID)).thenReturn(List.of());
        when(notificationPort.hasUnread(USER_ID)).thenReturn(false);
        when(bandPort.recommendTopBands(USER_ID, 10)).thenReturn(List.of(recommendedBand(20L)));
        when(performancePort.recommendPerformances(3)).thenReturn(List.of(performanceItem(100L)));

        FanHomeResponse response = service.getFanHome(USER_ID);

        assertEquals(false, response.hasFollowingBands());
        assertEquals(PerformanceSectionType.RECOMMENDED, response.performanceType());
        assertTrue(response.followingBandNews().isEmpty());
        assertEquals(1, response.recommendedBands().size());
        assertEquals(1, response.performances().size());
        // 팔로우가 없으면 소식·팔로우 공연 조회 자체를 하지 않는다
        verify(postPort, never()).findRecentNews(anyList(), anyInt());
        verify(performancePort, never()).findUpcomingByBandIds(anyList(), anyInt());
    }

    @Test
    void getFanHome_팔로우한_밴드가_있으면_소식과_다가오는_공연에_더해_추천_밴드도_반환한다() {
        when(followPort.findFollowingBandIds(USER_ID)).thenReturn(FOLLOWING_BAND_IDS);
        when(notificationPort.hasUnread(USER_ID)).thenReturn(false);
        when(postPort.findRecentNews(FOLLOWING_BAND_IDS, 5)).thenReturn(List.of());
        when(bandPort.recommendTopBands(USER_ID, 10)).thenReturn(List.of(recommendedBand(20L)));
        when(performancePort.findUpcomingByBandIds(FOLLOWING_BAND_IDS, 3))
                .thenReturn(List.of(performanceItem(100L)));

        FanHomeResponse response = service.getFanHome(USER_ID);

        assertEquals(true, response.hasFollowingBands());
        assertEquals(PerformanceSectionType.UPCOMING, response.performanceType());
        assertEquals(100L, response.performances().get(0).performanceId());
        // 팔로우가 있어도 추천 밴드는 항상 노출한다
        assertEquals(1, response.recommendedBands().size());
        assertEquals(20L, response.recommendedBands().get(0).bandId());
        verify(performancePort, never()).recommendPerformances(anyInt());
    }

    @Test
    void getFanHome_팔로우_밴드의_다가오는_공연이_없으면_추천_공연으로_대체한다() {
        when(followPort.findFollowingBandIds(USER_ID)).thenReturn(FOLLOWING_BAND_IDS);
        when(notificationPort.hasUnread(USER_ID)).thenReturn(false);
        when(postPort.findRecentNews(FOLLOWING_BAND_IDS, 5)).thenReturn(List.of());
        when(bandPort.recommendTopBands(USER_ID, 10)).thenReturn(List.of());
        when(performancePort.findUpcomingByBandIds(FOLLOWING_BAND_IDS, 3)).thenReturn(List.of());
        when(performancePort.recommendPerformances(3)).thenReturn(List.of(performanceItem(200L)));

        FanHomeResponse response = service.getFanHome(USER_ID);

        assertEquals(PerformanceSectionType.RECOMMENDED, response.performanceType());
        assertEquals(200L, response.performances().get(0).performanceId());
    }

    @Test
    void getFanHome_안읽은_알림_여부를_포함해_반환한다() {
        when(followPort.findFollowingBandIds(USER_ID)).thenReturn(List.of());
        when(notificationPort.hasUnread(USER_ID)).thenReturn(true);
        when(bandPort.recommendTopBands(USER_ID, 10)).thenReturn(List.of());
        when(performancePort.recommendPerformances(3)).thenReturn(List.of());

        FanHomeResponse response = service.getFanHome(USER_ID);

        assertEquals(true, response.hasUnreadNotification());
    }

    // ---------- getFollowingBandNews ----------

    @Test
    void getFollowingBandNews_페이지_크기를_상한으로_보정해_조회한다() {
        when(followPort.findFollowingBandIds(USER_ID)).thenReturn(FOLLOWING_BAND_IDS);

        service.getFollowingBandNews(USER_ID, 50L, 100);

        verify(postPort).findFollowingBandNews(FOLLOWING_BAND_IDS, 50L, 30);
    }

    @Test
    void getFollowingBandNews_사이즈가_0이면_최저_1로_보정한다() {
        when(followPort.findFollowingBandIds(USER_ID)).thenReturn(FOLLOWING_BAND_IDS);

        service.getFollowingBandNews(USER_ID, null, 0);

        verify(postPort).findFollowingBandNews(FOLLOWING_BAND_IDS, null, 1);
    }

    // ---------- getUpcomingPerformances ----------

    @Test
    void getUpcomingPerformances_보정된_페이징과_정렬로_조회한다() {
        when(followPort.findFollowingBandIds(USER_ID)).thenReturn(FOLLOWING_BAND_IDS);

        service.getUpcomingPerformances(USER_ID, UpcomingSortType.POPULAR, -1, 100);

        verify(performancePort).findUpcoming(USER_ID, FOLLOWING_BAND_IDS, UpcomingSortType.POPULAR, 0, 30);
    }

    // ---------- getPerformanceCalendar ----------

    @Test
    void getPerformanceCalendar_년월이_없으면_이번_달을_조회한다() {
        when(followPort.findFollowingBandIds(USER_ID)).thenReturn(FOLLOWING_BAND_IDS);
        YearMonth now = YearMonth.now();

        service.getPerformanceCalendar(USER_ID, null, null);

        verify(performancePort).findPerformanceDates(FOLLOWING_BAND_IDS, now.getYear(), now.getMonthValue());
    }

    @Test
    void getPerformanceCalendar_지정한_년월로_조회한다() {
        when(followPort.findFollowingBandIds(USER_ID)).thenReturn(FOLLOWING_BAND_IDS);

        service.getPerformanceCalendar(USER_ID, 2026, 12);

        verify(performancePort).findPerformanceDates(FOLLOWING_BAND_IDS, 2026, 12);
    }

    @Test
    void getPerformanceCalendar_월이_1과_12를_벗어나면_예외() {
        BaseException exception =
                assertThrows(BaseException.class, () -> service.getPerformanceCalendar(USER_ID, 2026, 13));

        assertEquals(GeneralErrorCode.INVALID_HTTP_MESSAGE_PARAMETER, exception.getBaseResponseCode());
        verify(performancePort, never()).findPerformanceDates(anyList(), anyInt(), anyInt());
    }

    // ---------- getPerformancesByDate ----------

    @Test
    void getPerformancesByDate_날짜가_없으면_오늘_기준으로_조회한다() {
        when(followPort.findFollowingBandIds(USER_ID)).thenReturn(FOLLOWING_BAND_IDS);

        service.getPerformancesByDate(USER_ID, null, 0, 10);

        verify(performancePort).findByDate(USER_ID, FOLLOWING_BAND_IDS, LocalDate.now(), 0, 10);
    }

    @Test
    void getPerformancesByDate_보정된_페이징으로_지정_날짜를_조회한다() {
        when(followPort.findFollowingBandIds(USER_ID)).thenReturn(FOLLOWING_BAND_IDS);
        LocalDate date = LocalDate.of(2026, 8, 15);

        service.getPerformancesByDate(USER_ID, date, -3, 0);

        verify(performancePort).findByDate(USER_ID, FOLLOWING_BAND_IDS, date, 0, 1);
    }
}
