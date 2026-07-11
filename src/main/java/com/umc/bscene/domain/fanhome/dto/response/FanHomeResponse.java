package com.umc.bscene.domain.fanhome.dto.response;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.fanhome.enums.PerformanceSectionType;
import com.umc.bscene.domain.post.enums.PostType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    // 팔로우한 밴드 소식 카드 한 개 (Post + 밴드 정보 조합)
    public record BandNewsItem(
            Long bandId,
            String bandName,
            String bandProfileImageUrl,
            Genre genre,              // 밴드 장르 (카드 상단 "장르 · 지역")
            Region region,            // 밴드 지역
            Long postId,              // 클릭 시 게시물 상세로 이동
            PostType type,            // 프론트가 사진/영상/텍스트 렌더 분기 (영상이면 재생아이콘)
            String imageUrl,          // 카드 미리보기 이미지 (PHOTO=첫 사진, VIDEO=썸네일(추후), TEXT=null)
            String title,
            String description,
            List<String> tags,        // 게시물 태그 (PostTag)
            LocalDateTime createdAt   // 상대시간("2시간 전")은 프론트가 계산
    ) {
    }

    // 이런 밴드는 어때요? 추천 밴드 카드 한 개
    public record RecommendedBandItem(
            Long bandId,
            String name,
            Genre genre,
            Region region,
            String profileImageUrl
    ) {
    }

    // 공연 카드 한 개 (다가오는 공연 / 추천 공연 공통)
    public record HomePerformanceItem(
            Long performanceId,
            String title,
            String venue,
            LocalDate performanceDate,
            LocalTime startTime,
            String posterImageUrl     // D-day는 프론트가 계산
    ) {
    }
}
