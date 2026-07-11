package com.umc.bscene.domain.band.adapter;

import com.umc.bscene.domain.band.service.BandRecommendationService;
import com.umc.bscene.domain.fanhome.dto.response.FanHomeResponse.RecommendedBandItem;
import com.umc.bscene.domain.fanhome.port.BandPort;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * 팬홈의 BandPort를 band 도메인이 구현하는 어댑터.
 * 기존 밴드 추천 로직(BandRecommendationService)을 재사용하고, 팬홈이 필요로 하는 필드만 추려 반환한다.
 */
@RequiredArgsConstructor
public class FanHomeAdapter implements BandPort {

    private final BandRecommendationService bandRecommendationService;

    @Override
    public List<RecommendedBandItem> recommendTopBands(Long userId, int limit) {
        // 첫 페이지에서 limit개만 조회 (cursor = null → 상위 점수부터)
        return bandRecommendationService.getRecommendedBands(userId, null, limit).bands().stream()
                .map(band -> new RecommendedBandItem(
                        band.bandId(),
                        band.name(),
                        band.genre(),
                        band.region(),
                        band.profileImageUrl()
                ))
                .toList();
    }
}
