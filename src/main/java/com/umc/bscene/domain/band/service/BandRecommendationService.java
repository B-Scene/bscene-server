package com.umc.bscene.domain.band.service;

import com.umc.bscene.domain.band.dto.response.BandRecommendResponse;

public interface BandRecommendationService {

    BandRecommendResponse getRecommendedBands(Long userId, Long cursor, Integer size);
}
