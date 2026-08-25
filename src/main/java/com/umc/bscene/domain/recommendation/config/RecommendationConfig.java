package com.umc.bscene.domain.recommendation.config;

import com.umc.bscene.domain.recommendation.adapter.BandAdapter;
import com.umc.bscene.domain.recommendation.repository.BandInteractionRepository;
import com.umc.bscene.domain.recommendation.repository.BandRecommendationLogRepository;
import com.umc.bscene.domain.recommendation.repository.BandSimilarityRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RecommendationConfig {

    // 밴드 도메인 RecommendationPort 구현 어댑터 (검수 밴드 삭제 시 추천 파생 데이터 정리)
    @Bean
    public BandAdapter bandRecommendationAdapter(
            BandInteractionRepository bandInteractionRepository,
            BandRecommendationLogRepository bandRecommendationLogRepository,
            BandSimilarityRepository bandSimilarityRepository
    ) {
        return new BandAdapter(
                bandInteractionRepository,
                bandRecommendationLogRepository,
                bandSimilarityRepository
        );
    }
}
