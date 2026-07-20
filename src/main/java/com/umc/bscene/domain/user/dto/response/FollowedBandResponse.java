package com.umc.bscene.domain.user.dto.response;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;

import java.util.List;

// 팔로우한 밴드 목록 조회 응답 (offset 기반 무한스크롤, 밴드명 가나다순)
public record FollowedBandResponse(
        Long totalCount,                        // 첫 페이지(page 0)에서만 — 상단 "팔로우한 밴드 N팀" 표시용 (이후 페이지는 null)
        List<FollowedBandItem> items,
        int page,                               // 현재 페이지 (0-base)
        boolean hasNext                         // 다음 페이지 존재 여부
) {

    // 팔로우한 밴드 목록 아이템 — 팔로잉 버튼(언팔)은 bandId로 기존 DELETE /bands/{bandId}/follow 호출
    public record FollowedBandItem(
            Long bandId,
            String name,
            Genre genre,
            Region region,
            String profileImageUrl,
            long followerCount                  // 밴드의 전체 팔로워 수
    ) {
    }
}
