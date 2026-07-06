package com.umc.bscene.domain.stream.config;

import com.umc.bscene.domain.stream.controller.MediaMtxController;
import com.umc.bscene.domain.stream.service.StreamService;
import com.umc.bscene.domain.stream.service.StreamServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StreamConfig {

    @Bean
    public StreamService streamService() {
        return new StreamServiceImpl();
    }

    @Bean
    public MediaMtxController mediaMtxController(StreamService streamService) {
        return new MediaMtxController(streamService);
    }
}
