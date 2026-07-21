package com.umc.bscene.domain.user.config;

import com.umc.bscene.domain.user.adapter.StreamAdapter;
import com.umc.bscene.domain.user.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserConfig {

    @Bean
    public StreamAdapter streamUserAdapter(UserRepository userRepository) {
        return new StreamAdapter(userRepository);
    }
}