package com.umc.bscene.domain.user.config;

import com.umc.bscene.domain.user.adapter.PostAdapter;
import com.umc.bscene.domain.user.adapter.StreamAdapter;
import com.umc.bscene.domain.user.repository.FanProfileRepository;
import com.umc.bscene.domain.user.repository.UserBlockRepository;
import com.umc.bscene.domain.user.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserConfig {

    @Bean
    public StreamAdapter streamUserAdapter(
            UserRepository userRepository,
            FanProfileRepository fanProfileRepository
    ) {
        return new StreamAdapter(userRepository, fanProfileRepository);
    }

    // 게시물 도메인 UserPort 구현 어댑터 (댓글 목록의 차단 유저 필터·작성자 팬 프로필 조회)
    @Bean
    public PostAdapter postUserAdapter(
            UserBlockRepository userBlockRepository,
            FanProfileRepository fanProfileRepository
    ) {
        return new PostAdapter(userBlockRepository, fanProfileRepository);
    }
}