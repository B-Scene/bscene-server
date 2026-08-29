package com.umc.bscene.domain.band.service;

import com.umc.bscene.domain.band.dto.response.BandRecommendResponse;

public interface BandRecommendationService {

    BandRecommendResponse getRecommendedBands(Long userId, Long cursor, Integer size, boolean withDummy);

    // withDummy 생략 호출은 더미 밴드를 포함한 기존 동작을 유지한다 (팬홈 등 기존 호출부 호환)
    default BandRecommendResponse getRecommendedBands(Long userId, Long cursor, Integer size) {
        return getRecommendedBands(userId, cursor, size, true);
    }
}
