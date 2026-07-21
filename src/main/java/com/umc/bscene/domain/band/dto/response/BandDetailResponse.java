package com.umc.bscene.domain.band.dto.response;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.band.entity.Band;

// 팬모드 밴드 상세(프로필 화면) 조회 응답 — 밴드 기본 정보 + 팔로우 버튼/라이브 입장 버튼 상태
public record BandDetailResponse(
        Long bandId,
        String name,
        Genre genre,
        Region region,
        String profileImageUrl,
        String description,
        Long followerCount,
        boolean isFollowing,                // 요청한 사용자의 팔로우 여부 (팔로우/팔로잉 버튼 상태)
        boolean isLive,                     // 현재 라이브(OPEN) 진행 중 여부
        Long liveId                         // 라이브 중이면 입장(POST /lives/{liveId})에 쓸 ID, 아니면 null
) {

    public static BandDetailResponse of(Band band, Long followerCount, boolean isFollowing, Long liveId) {
        return new BandDetailResponse(
                band.getId(),
                band.getName(),
                band.getGenre(),
                band.getRegion(),
                band.getProfileImageUrl(),
                band.getDescription(),
                followerCount,
                isFollowing,
                liveId != null,
                liveId
        );
    }
}
