package com.umc.bscene.domain.user.dto.response;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.user.enums.UserMode;

import java.util.List;

public record FanMyPageResponse(
        String nickname,
        List<Genre> genres,                   // 관심 장르 (복수)
        List<Region> regions,                 // 활동 지역 (복수)
        UserMode currentMode,                 // 현재 모드 (FAN | BAND)
        long followingCount,                  // 팔로우한 밴드 수
        long interestedPerformanceCount,      // 관심 등록한 공연 수
        long participatedPerformanceCount     // 참여 공연 수 (참여 완료 처리한 공연)
) {
    public static FanMyPageResponse of(
            String nickname,
            List<Genre> genres,
            List<Region> regions,
            UserMode currentMode,
            long followingCount,
            long interestedPerformanceCount,
            long participatedPerformanceCount
    ) {
        return new FanMyPageResponse(
                nickname,
                genres,
                regions,
                currentMode,
                followingCount,
                interestedPerformanceCount,
                participatedPerformanceCount
        );
    }
}
