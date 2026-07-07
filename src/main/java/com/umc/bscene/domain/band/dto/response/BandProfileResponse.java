package com.umc.bscene.domain.band.dto.response;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.band.entity.Band;

public record BandProfileResponse(
        Long bandId,
        String name,
        Genre genre,
        Region region,
        String profileImageUrl,
        String description,
        Long followerCount,
        Long memberCount,
        Long performanceCount
) {

    public static BandProfileResponse of(
            Band band,
            Long followerCount,
            Long memberCount,
            Long performanceCount
    ) {
        return new BandProfileResponse(
                band.getId(),
                band.getName(),
                band.getGenre(),
                band.getRegion(),
                band.getProfileImageUrl(),
                band.getDescription(),
                followerCount,
                memberCount,
                performanceCount
        );
    }

}
