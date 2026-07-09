package com.umc.bscene.domain.fanhome.service;

import com.umc.bscene.domain.fanhome.dto.response.BandNewsItem;
import com.umc.bscene.domain.fanhome.dto.response.FanHomeResponse;
import com.umc.bscene.domain.fanhome.dto.response.HomePerformanceItem;
import com.umc.bscene.domain.fanhome.dto.response.RecommendedBandItem;
import com.umc.bscene.domain.fanhome.enums.PerformanceSectionType;
import com.umc.bscene.domain.fanhome.port.BandNewsPort;
import com.umc.bscene.domain.fanhome.port.BandRecommendPort;
import com.umc.bscene.domain.fanhome.port.FollowQueryPort;
import com.umc.bscene.domain.fanhome.port.PerformanceRecommendPort;
import com.umc.bscene.domain.fanhome.port.UpcomingPerformancePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FanHomeService {

    private static final int BAND_NEWS_LIMIT = 2;         // 팔로우한 밴드 소식 최대 2개
    private static final int RECOMMENDED_BAND_LIMIT = 5;  // 이런 밴드는 어때요? 최대 5개
    private static final int PERFORMANCE_LIMIT = 3;       // 공연 섹션 최대 3개

    private final FollowQueryPort followQueryPort;
    private final BandNewsPort bandNewsPort;
    private final BandRecommendPort bandRecommendPort;
    private final UpcomingPerformancePort upcomingPerformancePort;
    private final PerformanceRecommendPort performanceRecommendPort;

    // 팬모드 홈 통합 조회
    public FanHomeResponse getFanHome(Long userId) {
        // 팔로우한 밴드 조회
        List<Long> followingBandIds = followQueryPort.findFollowingBandIds(userId);
        boolean hasFollowingBands = !followingBandIds.isEmpty();

        // 팔로우한 밴드 소식 (팔로우 없으면 빈 리스트 → 프론트가 "아직 팔로우한 밴드가 없어요")
        List<BandNewsItem> followingBandNews = hasFollowingBands
                ? bandNewsPort.findRecentNews(followingBandIds, BAND_NEWS_LIMIT)
                : List.of();

        // 이런 밴드는 어때요? (팔로우하는 밴드가 없는 사용자에게만 노출)
        List<RecommendedBandItem> recommendedBands = hasFollowingBands
                ? List.of()
                : bandRecommendPort.recommendTopBands(userId, RECOMMENDED_BAND_LIMIT);

        // 팔로우한 밴드들의 다가오는 공연 (팔로우 없으면 빈 리스트)
        List<HomePerformanceItem> upcomingPerformances = hasFollowingBands
                ? upcomingPerformancePort.findUpcomingByBandIds(followingBandIds, PERFORMANCE_LIMIT)
                : List.of();

        // 다가오는 공연이 없으면 추천 공연으로 대체
        PerformanceSectionType performanceType;
        List<HomePerformanceItem> performances;
        if (upcomingPerformances.isEmpty()) {
            performanceType = PerformanceSectionType.RECOMMENDED;
            performances = performanceRecommendPort.recommendPerformances(userId, PERFORMANCE_LIMIT);
        } else {
            performanceType = PerformanceSectionType.UPCOMING;
            performances = upcomingPerformances;
        }

        return FanHomeResponse.of(
                hasFollowingBands,
                performanceType,
                followingBandNews,
                recommendedBands,
                performances
        );
    }
}
