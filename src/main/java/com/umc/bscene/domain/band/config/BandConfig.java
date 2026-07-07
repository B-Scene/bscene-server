package com.umc.bscene.domain.band.config;

import com.umc.bscene.domain.band.port.FollowPort;
import com.umc.bscene.domain.band.port.PerformancePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BandConfig {

    @Bean
    public FollowPort followPort() {
        return new FollowPort() {
            @Override
            public Long countFollowersByBandId(Long bandId) {
                return 0L;
            }
        };
    }

    @Bean
    public PerformancePort performancePort() {
        return new PerformancePort() {
            @Override
            public Long countPerformancesByBandId(Long bandId) {
                return 0L;
            }
        };
    }
}