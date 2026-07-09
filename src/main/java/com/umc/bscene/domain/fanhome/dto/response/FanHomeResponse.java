package com.umc.bscene.domain.fanhome.dto.response;

import com.umc.bscene.domain.fanhome.enums.PerformanceSectionType;

import java.util.List;

public record FanHomeResponse(
        boolean hasFollowingBands,
        PerformanceSectionType performanceType,   // UPCOMING | RECOMMENDED

        // 섹션 1: 팔로우한 밴드 소식
        List<BandNewsItem> followingBandNews,

        // 섹션 2: 이런 밴드는 어때요? (팔로우하는 밴드가 없을 때만 채움, 아니면 빈 리스트)
        List<RecommendedBandItem> recommendedBands,

        // 섹션 3: 공연
        List<HomePerformanceItem> performances
) {
    public static FanHomeResponse of(
            boolean hasFollowingBands,
            PerformanceSectionType performanceType,
            List<BandNewsItem> followingBandNews,
            List<RecommendedBandItem> recommendedBands,
            List<HomePerformanceItem> performances
    ) {
        return new FanHomeResponse(
                hasFollowingBands,
                performanceType,
                followingBandNews,
                recommendedBands,
                performances
        );
    }
}
