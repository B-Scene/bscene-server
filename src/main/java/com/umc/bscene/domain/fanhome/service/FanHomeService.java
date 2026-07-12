package com.umc.bscene.domain.fanhome.service;

import com.umc.bscene.domain.fanhome.dto.response.FanHomeResponse.BandNewsItem;
import com.umc.bscene.domain.fanhome.dto.response.FanHomeResponse;
import com.umc.bscene.domain.fanhome.dto.response.FanHomeResponse.HomePerformanceItem;
import com.umc.bscene.domain.fanhome.dto.response.FanHomeResponse.RecommendedBandItem;
import com.umc.bscene.domain.fanhome.dto.response.FollowingBandNewsResponse;
import com.umc.bscene.domain.fanhome.dto.response.UpcomingPerformanceResponse;
import com.umc.bscene.domain.fanhome.enums.PerformanceSectionType;
import com.umc.bscene.domain.fanhome.enums.UpcomingSortType;
import com.umc.bscene.domain.fanhome.port.BandPort;
import com.umc.bscene.domain.fanhome.port.FollowPort;
import com.umc.bscene.domain.fanhome.port.NotificationPort;
import com.umc.bscene.domain.fanhome.port.PerformancePort;
import com.umc.bscene.domain.fanhome.port.PostPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FanHomeService {

    private static final int BAND_NEWS_LIMIT = 5;         // 팔로우한 밴드 소식 최대 5개
    private static final int RECOMMENDED_BAND_LIMIT = 5;  // 이런 밴드는 어때요? 최대 5개
    private static final int PERFORMANCE_LIMIT = 3;       // 공연 섹션 최대 3개
    private static final int MAX_NEWS_PAGE_SIZE = 30;     // 소식 전체조회 페이지 크기 상한
    private static final int MAX_PERFORMANCE_PAGE_SIZE = 30; // 다가오는 공연 목록 페이지 크기 상한

    private final FollowPort followPort;
    private final PostPort postPort;
    private final BandPort bandPort;
    private final PerformancePort performancePort;
    private final NotificationPort notificationPort;

    // 팬모드 홈 통합 조회
    public FanHomeResponse getFanHome(Long userId) {
        // 팔로우한 밴드 조회
        List<Long> followingBandIds = followPort.findFollowingBandIds(userId);
        boolean hasFollowingBands = !followingBandIds.isEmpty();

        // 안읽은 알림 존재 여부 (알림 뱃지 노출용)
        boolean hasUnreadNotification = notificationPort.hasUnread(userId);

        // 팔로우한 밴드 소식 (팔로우 없으면 빈 리스트 → 프론트가 "아직 팔로우한 밴드가 없어요")
        List<BandNewsItem> followingBandNews = hasFollowingBands
                ? postPort.findRecentNews(followingBandIds, BAND_NEWS_LIMIT)
                : List.of();

        // 이런 밴드는 어때요? (팔로우하는 밴드가 없는 사용자에게만 노출)
        List<RecommendedBandItem> recommendedBands = hasFollowingBands
                ? List.of()
                : bandPort.recommendTopBands(userId, RECOMMENDED_BAND_LIMIT);

        // 팔로우한 밴드들의 다가오는 공연 (팔로우 없으면 빈 리스트)
        List<HomePerformanceItem> upcomingPerformances = hasFollowingBands
                ? performancePort.findUpcomingByBandIds(followingBandIds, PERFORMANCE_LIMIT)
                : List.of();

        // 다가오는 공연이 없으면 추천 공연으로 대체
        PerformanceSectionType performanceType;
        List<HomePerformanceItem> performances;
        if (upcomingPerformances.isEmpty()) {
            performanceType = PerformanceSectionType.RECOMMENDED;
            performances = performancePort.recommendPerformances(PERFORMANCE_LIMIT);
        } else {
            performanceType = PerformanceSectionType.UPCOMING;
            performances = upcomingPerformances;
        }

        return FanHomeResponse.of(
                hasFollowingBands,
                hasUnreadNotification,
                performanceType,
                followingBandNews,
                recommendedBands,
                performances
        );
    }

    // 팔로우한 밴드 소식 전체 조회 (id 커서 기반 무한스크롤)
    public FollowingBandNewsResponse getFollowingBandNews(Long userId, Long cursor, int size) {
        int pageSize = Math.min(Math.max(size, 1), MAX_NEWS_PAGE_SIZE);
        List<Long> followingBandIds = followPort.findFollowingBandIds(userId);
        return postPort.findFollowingBandNews(followingBandIds, cursor, pageSize);
    }

    // 다가오는 공연 전체 목록 (offset 기반 무한스크롤, 임박/최신/인기순)
    public UpcomingPerformanceResponse getUpcomingPerformances(Long userId, UpcomingSortType sort, int page, int size) {
        int pageNumber = Math.max(page, 0);
        int pageSize = Math.min(Math.max(size, 1), MAX_PERFORMANCE_PAGE_SIZE);
        List<Long> followingBandIds = followPort.findFollowingBandIds(userId);
        return performancePort.findUpcoming(userId, followingBandIds, sort, pageNumber, pageSize);
    }
}
