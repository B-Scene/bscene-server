package com.umc.bscene.domain.band.dto.request;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;

public record BandUpdateRequest(
        String name,
        Genre genre,
        Region region,
        String profileImageUrl,
        String description
) {
}
