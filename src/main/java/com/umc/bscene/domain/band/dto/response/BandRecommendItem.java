package com.umc.bscene.domain.band.dto.response;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.band.entity.Band;

public record BandRecommendItem(
        Long bandId,
        String name,
        Genre genre,
        Region region,
        String profileImageUrl,
        String description,
        boolean isFollowing,
        int score
) {
    // 팔로우 기능이 없어 isFollowing은 항상 false로 내려감
    public static BandRecommendItem of(Band band, int score) {
        return new BandRecommendItem(
                band.getId(),
                band.getName(),
                band.getGenre(),
                band.getRegion(),
                band.getProfileImageUrl(),
                band.getDescription(),
                false,
                score
        );
    }
}
