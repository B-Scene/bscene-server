package com.umc.bscene.domain.follow.config;

import com.umc.bscene.domain.follow.adapter.FanHomeAdapter;
import com.umc.bscene.domain.follow.adapter.BandAdapter;
import com.umc.bscene.domain.follow.repository.FollowRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FollowConfig {

    @Bean
    public BandAdapter bandFollowAdapter(FollowRepository followRepository) {
        return new BandAdapter(followRepository);
    }

    @Bean
    public FanHomeAdapter fanHomeFollowAdapter(FollowRepository followRepository) {
        return new FanHomeAdapter(followRepository);
    }
}
