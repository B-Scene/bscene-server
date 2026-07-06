package com.umc.bscene.domain.stream.config;

import com.umc.bscene.domain.stream.controller.MediaMtxController;
import com.umc.bscene.domain.stream.repository.AudioStreamRepository;
import com.umc.bscene.domain.stream.repository.StreamMemberRepository;
import com.umc.bscene.domain.stream.service.StreamService;
import com.umc.bscene.domain.stream.service.StreamServiceImpl;
import com.umc.bscene.global.security.util.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class StreamConfig {

    @Bean
    public StreamService streamService(
            JwtUtil jwtUtil,
            AudioStreamRepository audioStreamRepository,
            StreamMemberRepository streamMemberRepository,
            StringRedisTemplate stringRedisTemplate
    ) {
        return new StreamServiceImpl(
                jwtUtil,
                audioStreamRepository,
                streamMemberRepository,
                stringRedisTemplate
        );
    }

    @Bean
    public MediaMtxController mediaMtxController(StreamService streamService) {
        return new MediaMtxController(streamService);
    }
}
