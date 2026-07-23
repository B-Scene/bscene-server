package com.umc.bscene.domain.user.dto.response;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;

public record MyBandProfile(
        Long bandId,
        Long bandMemberProfileId,
        String profileImageUrl,
        String bandName,
        String genre,       // Genre enum에서 String 변환 필요
        String region,      // Region enum에서 String 변환 필요
        Boolean isActive
){
    public MyBandProfile(Long bandId, Long bandMemberProfileId, String profileImageUrl, String bandName, Genre genre, Region region, Boolean isActive) {
        this(bandId, bandMemberProfileId, profileImageUrl, bandName, genre.getName(), region.getName(), isActive);
    }
}
