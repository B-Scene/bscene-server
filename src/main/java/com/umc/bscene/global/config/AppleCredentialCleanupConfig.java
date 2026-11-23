package com.umc.bscene.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration(proxyBeanMethods = false)
public class AppleCredentialCleanupConfig {

    // 전용 scheduler 등록으로 Boot 자동 구성이 빠져도 기존 라이브 작업은 기본 스케줄러에 남긴다.
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        return createScheduler(1, "scheduling-");
    }

    @Bean
    public ThreadPoolTaskScheduler appleCredentialTaskScheduler() {
        return createScheduler(1, "apple-credential-cleanup-");
    }

    private ThreadPoolTaskScheduler createScheduler(int poolSize, String threadNamePrefix) {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(poolSize);
        scheduler.setThreadNamePrefix(threadNamePrefix);
        return scheduler;
    }
}
