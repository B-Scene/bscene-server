package com.umc.bscene.domain.performance.config;

import com.umc.bscene.domain.performance.adapter.FanHomeAdapter;
import com.umc.bscene.domain.performance.adapter.PerformanceAdapter;
import com.umc.bscene.domain.performance.repository.PerformanceRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PerformancePortConfig {

    @Bean
    public PerformanceAdapter PerformanceAdapter(
            PerformanceRepository performanceRepository
    ) {
        return new PerformanceAdapter(performanceRepository);
    }

    // 팬홈 PerformancePort 구현 어댑터 (다가오는 공연 / 관심수 기반 추천 공연)
    @Bean
    public FanHomeAdapter fanHomePerformanceAdapter(
            PerformanceRepository performanceRepository
    ) {
        return new FanHomeAdapter(performanceRepository);
    }
}