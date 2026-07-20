package com.umc.bscene.domain.user.dto.response;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;

public record MyBandProfile(
        Long bandId,
        String profileImageUrl,
        String bandName,
        Genre genre,       // Genre enum에서 String 변환 필요
        Region region,      // Region enum에서 String 변환 필요
        Boolean isActive
){ }
