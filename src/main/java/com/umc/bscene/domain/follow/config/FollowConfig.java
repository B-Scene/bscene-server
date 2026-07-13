package com.umc.bscene.domain.follow.config;

import com.umc.bscene.domain.follow.adapter.FanHomeAdapter;
import com.umc.bscene.domain.follow.adapter.BandAdapter;
import com.umc.bscene.domain.follow.adapter.StreamAdapter;
import com.umc.bscene.domain.follow.adapter.UserAdapter;
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

    // 마이페이지 FollowPort 구현 어댑터 (팔로우한 밴드 수)
    @Bean
    public UserAdapter userFollowAdapter(FollowRepository followRepository) {
        return new UserAdapter(followRepository);
    }

    // 스트림 FollowPort 구현 어댑터 (팔로우한 밴드 목록·밴드 팔로워 목록)
    @Bean
    public StreamAdapter streamFollowAdapter(FollowRepository followRepository) {
        return new StreamAdapter(followRepository);
    }
}
