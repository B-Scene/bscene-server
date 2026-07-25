package com.umc.bscene.domain.notification.config;

import com.google.firebase.messaging.FirebaseMessaging;
import com.umc.bscene.domain.notification.adapter.BandAdapter;
import com.umc.bscene.domain.notification.adapter.ChatAdapter;
import com.umc.bscene.domain.notification.adapter.FanHomeAdapter;
import com.umc.bscene.domain.notification.adapter.FirebasePushAdapter;
import com.umc.bscene.domain.notification.adapter.NoopPushAdapter;
import com.umc.bscene.domain.notification.adapter.NotificationAdapter;
import com.umc.bscene.domain.notification.adapter.PerformanceAdapter;
import com.umc.bscene.domain.notification.adapter.PostAdapter;
import com.umc.bscene.domain.notification.adapter.SessionAdapter;
import com.umc.bscene.domain.notification.adapter.StreamAdapter;
import com.umc.bscene.domain.notification.adapter.UserAdapter;
import com.umc.bscene.domain.notification.port.PushPort;
import com.umc.bscene.domain.notification.repository.NotificationRepository;
import com.umc.bscene.domain.notification.scheduler.NotificationCleanupScheduler;
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

    // fanhome도메인 NotificationPort 구현 어댑터 (안읽은 알림 존재 여부)
    @Bean
    public FanHomeAdapter fanHomeNotificationAdapter(NotificationRepository notificationRepository) {
        return new FanHomeAdapter(notificationRepository);
    }

    // Session 도메인 NotificationPort 구현 어댑터
    @Bean
    public SessionAdapter sessionNotificationAdapter(NotificationPort notificationPort) {
        return new SessionAdapter(notificationPort);
    }

    // Stream 도메인 NotifyPort 구현 어댑터
    @Bean
    public StreamAdapter streamNotificationAdapter(NotificationPort notificationPort) {
        return new StreamAdapter(notificationPort);
    }

    // Performance 도메인 NotificationPort 구현 어댑터
    @Bean
    public PerformanceAdapter performanceNotificationAdapter(NotificationPort notificationPort) {
        return new PerformanceAdapter(notificationPort);
    }

    // Post 도메인 NotifyPort 구현 어댑터
    @Bean
    public PostAdapter postNotificationAdapter(NotificationPort notificationPort) {
        return new PostAdapter(notificationPort);
    }

    // Chat 도메인 NotifyPort 구현 어댑터
    @Bean
    public ChatAdapter chatNotificationAdapter(NotificationPort notificationPort) {
        return new ChatAdapter(notificationPort);
    }

    @Bean
    public NotificationCleanupScheduler notificationCleanupScheduler(
            NotificationService notificationService
    ) {
        return new NotificationCleanupScheduler(notificationService);
    }

    // Band 도메인 NotifyPort 구현 어댑터
    @Bean
    public BandAdapter bandNotificationAdapter(
            NotificationPort notificationPort
    ) {
        return new BandAdapter(notificationPort);
    }

    // User 도메인 NotifyPort 구현 어댑터
    @Bean
    public UserAdapter userNotificationAdapter(NotificationPort notificationPort) {
        return new UserAdapter(notificationPort);
    }
}
