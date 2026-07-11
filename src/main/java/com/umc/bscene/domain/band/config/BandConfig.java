package com.umc.bscene.domain.band.config;

import com.umc.bscene.domain.band.adapter.FanHomeAdapter;
import com.umc.bscene.domain.band.service.BandRecommendationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BandConfig {

    @Bean
    public FanHomeAdapter fanHomeBandAdapter(BandRecommendationService bandRecommendationService) {
        return new FanHomeAdapter(bandRecommendationService);
    }
}
