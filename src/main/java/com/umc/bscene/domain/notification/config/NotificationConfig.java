package com.umc.bscene.domain.notification.config;

import com.umc.bscene.domain.notification.port.PushSender;
import com.umc.bscene.domain.notification.service.NoopPushSender;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationConfig {

    @Bean
    public PushSender pushSender() {
        return new NoopPushSender();
    }
}