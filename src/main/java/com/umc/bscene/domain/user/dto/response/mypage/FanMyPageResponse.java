package com.umc.bscene.domain.user.dto.response.mypage;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.user.enums.UserMode;

import java.util.List;

public record FanMyPageResponse(
        String nickname,
        Genre genre,                          // 대표 장르 — 등록한 관심장르 중 팔로우한 밴드가 많은 장르 (동점/팔로우 없으면 먼저 고른 장르)
        int additionalGenreCount,             // 대표를 제외한 나머지 관심장르 수 — 프론트에서 "록 외 2" 형태로 표시
        List<Region> regions,                 // 활동 지역 (복수)
        UserMode currentMode,                 // 현재 모드 (FAN | BAND)
        long followingCount,                  // 팔로우한 밴드 수
        long interestedPerformanceCount,      // 관심 등록한 공연 수
        long participatedPerformanceCount     // 참여 공연 수 (참여 완료 처리한 공연)
) implements MyPageResponse {
    public static FanMyPageResponse of(
            String nickname,
            Genre genre,
            int additionalGenreCount,
            List<Region> regions,
            UserMode currentMode,
            long followingCount,
            long interestedPerformanceCount,
            long participatedPerformanceCount
    ) {
        return new FanMyPageResponse(
                nickname,
                genre,
                additionalGenreCount,
                regions,
                currentMode,
                followingCount,
                interestedPerformanceCount,
                participatedPerformanceCount
        );
    }
}
