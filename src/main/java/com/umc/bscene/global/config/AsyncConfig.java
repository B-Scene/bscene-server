package com.umc.bscene.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    // 영상 첫 프레임 썸네일 생성처럼 무거운 후처리 작업 전용 스레드풀
    @Bean(name = "postThumbnailExecutor")
    public Executor postThumbnailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("post-thumbnail-");
        executor.initialize();
        return executor;
    }
}
