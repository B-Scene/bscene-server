package com.umc.bscene.domain.band.dto.response;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.band.entity.Band;

public record BandResponse(
        Long id,
        Long ownerId,
        String name,
        Genre genre,
        Region region,
        String profileImageUrl,
        String description
) {
    public static BandResponse from(Band band) {
        return new BandResponse(
                band.getId(),
                band.getOwner().getId(),
                band.getName(),
                band.getGenre(),
                band.getRegion(),
                band.getProfileImageUrl(),
                band.getDescription()
        );
    }
}
