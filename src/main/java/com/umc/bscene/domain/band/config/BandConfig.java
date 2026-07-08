package com.umc.bscene.domain.band.config;

import com.umc.bscene.domain.band.port.FollowPort;
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
}