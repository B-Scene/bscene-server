package com.umc.bscene.domain.recommendation.event;

import java.util.List;

/**
 * 밴드 추천 목록이 사용자에게 노출됐을 때 발행 — 추천 노출 로그(BandRecommendationLog) 적재용.
 */
public record BandRecommendationExposedEvent(
        Long userId,
        List<Exposure> exposures
) {
    public record Exposure(Long bandId, int position, String algorithmVersion) {
    }
}
