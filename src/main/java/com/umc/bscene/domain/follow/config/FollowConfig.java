package com.umc.bscene.domain.follow.config;

import com.umc.bscene.domain.follow.adapter.FollowPortAdapter;
import com.umc.bscene.domain.follow.adapter.FollowQueryPortAdapter;
import com.umc.bscene.domain.follow.repository.FollowRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FollowConfig {

    @Bean
    public FollowPortAdapter followPortAdapter(FollowRepository followRepository) {
        return new FollowPortAdapter(followRepository);
    }

    @Bean
    public FollowQueryPortAdapter followQueryPortAdapter(FollowRepository followRepository) {
        return new FollowQueryPortAdapter(followRepository);
    }
}
