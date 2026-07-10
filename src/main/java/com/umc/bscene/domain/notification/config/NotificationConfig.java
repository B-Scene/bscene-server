package com.umc.bscene.domain.notification.config;

import com.google.firebase.messaging.FirebaseMessaging;
import com.umc.bscene.domain.notification.adapter.NotificationAdapter;
import com.umc.bscene.domain.notification.port.PushPort;
import com.umc.bscene.domain.notification.adapter.FirebasePushAdapter;
import com.umc.bscene.domain.notification.adapter.NoopPushAdapter;
import com.umc.bscene.domain.notification.service.NotificationService;
import com.umc.bscene.global.notification.port.NotificationPort;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationConfig {

    @Bean
    public PushPort pushPort(
            @Value("${notification.fcm.enabled:false}") boolean fcmEnabled,
            ObjectProvider<FirebaseMessaging> firebaseMessagingProvider
    ) {
        if (!fcmEnabled) {
            return new NoopPushAdapter();
        }

        return new FirebasePushAdapter(firebaseMessagingProvider.getObject());
    }

    @Bean
    public NotificationPort notificationPort(NotificationService notificationService) {
        return new NotificationAdapter(notificationService);
    }
}