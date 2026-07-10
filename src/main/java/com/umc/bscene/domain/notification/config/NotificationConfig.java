package com.umc.bscene.domain.notification.config;

import com.google.firebase.messaging.FirebaseMessaging;
import com.umc.bscene.domain.notification.port.PushSender;
import com.umc.bscene.domain.notification.service.FirebasePushSender;
import com.umc.bscene.domain.notification.service.NoopPushSender;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationConfig {

    @Bean
    public PushSender pushSender(
            @Value("${notification.fcm.enabled:false}") boolean fcmEnabled,
            ObjectProvider<FirebaseMessaging> firebaseMessagingProvider
    ) {
        if (!fcmEnabled) {
            return new NoopPushSender();
        }

        return new FirebasePushSender(firebaseMessagingProvider.getObject());
    }
}