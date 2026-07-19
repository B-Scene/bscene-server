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
        double score,
        String reason
) {
    public static BandRecommendItem of(Band band, double score, String reason) {
        return new BandRecommendItem(
                band.getId(),
                band.getName(),
                band.getGenre(),
                band.getRegion(),
                band.getProfileImageUrl(),
                band.getDescription(),
                score,
                reason
        );
    }
}
