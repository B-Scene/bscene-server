package com.umc.bscene.domain.fanhome.dto.response;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;

// 이런 밴드는 어때요? 추천 밴드 카드 한 개
public record RecommendedBandItem(
        Long bandId,
        String name,
        Genre genre,
        Region region,
        String profileImageUrl
) {
}
