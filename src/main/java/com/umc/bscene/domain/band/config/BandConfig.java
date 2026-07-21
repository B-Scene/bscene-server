package com.umc.bscene.domain.band.config;

import com.umc.bscene.domain.band.adapter.FanHomeAdapter;
import com.umc.bscene.domain.band.adapter.SearchAdapter;
import com.umc.bscene.domain.band.adapter.SessionAdapter;
import com.umc.bscene.domain.band.port.StreamPort;
import com.umc.bscene.domain.band.repository.BandMemberRepository;
import com.umc.bscene.domain.band.repository.BandRepository;
import com.umc.bscene.domain.band.service.BandRecommendationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
public class BandConfig {

    @Bean
    public FanHomeAdapter fanHomeBandAdapter(BandRecommendationService bandRecommendationService) {
        return new FanHomeAdapter(bandRecommendationService);
    }

    // 검색 색인 BandPort 구현 어댑터
    @Bean
    public SearchAdapter searchBandAdapter(BandRepository bandRepository) {
        return new SearchAdapter(bandRepository);
    }

    // 세션 도메인 BandMemberPort 구현 어댑터
    @Bean
    public SessionAdapter sessionBandMemberAdapter(BandMemberRepository bandMemberRepository) {
        return new SessionAdapter(bandMemberRepository);
    }
}
