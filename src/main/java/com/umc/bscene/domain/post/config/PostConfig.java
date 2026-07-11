package com.umc.bscene.domain.post.config;

import com.umc.bscene.domain.post.adapter.FanHomeAdapter;
import com.umc.bscene.domain.post.repository.PostRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PostConfig {

    // 팬홈 PostPort 구현 어댑터 (팔로우 밴드 최근 소식)
    @Bean
    public FanHomeAdapter fanHomePostAdapter(PostRepository postRepository) {
        return new FanHomeAdapter(postRepository);
    }
}
