package com.umc.bscene.domain.recommendation.adapter;

import com.umc.bscene.domain.band.port.RecommendationPort;
import com.umc.bscene.domain.recommendation.repository.BandInteractionRepository;
import com.umc.bscene.domain.recommendation.repository.BandRecommendationLogRepository;
import com.umc.bscene.domain.recommendation.repository.BandSimilarityRepository;
import lombok.RequiredArgsConstructor;

/**
 * 밴드 도메인의 RecommendationPort를 recommendation 도메인이 구현하는 어댑터.
 * 검수 플로우의 밴드 삭제 시 FK로 삭제를 막는 추천 파생 데이터를 정리한다.
 */
@RequiredArgsConstructor
public class BandAdapter implements RecommendationPort {

    private final BandInteractionRepository bandInteractionRepository;
    private final BandRecommendationLogRepository bandRecommendationLogRepository;
    private final BandSimilarityRepository bandSimilarityRepository;

    @Override
    public void deleteAllByBandId(Long bandId) {
        bandInteractionRepository.deleteAllByBandId(bandId);
        bandRecommendationLogRepository.deleteAllByBandId(bandId);
        bandSimilarityRepository.deleteAllByBandId(bandId);
    }
}
