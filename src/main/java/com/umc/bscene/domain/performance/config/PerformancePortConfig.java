package com.umc.bscene.domain.performance.config;

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
}