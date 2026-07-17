package com.umc.bscene.domain.notification.scheduler;

import com.umc.bscene.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.time.ZoneId;

@RequiredArgsConstructor
public class NotificationCleanupScheduler {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final NotificationService notificationService;

    // 매시간 정각에 보관 기간이 지난 읽은 알림을 삭제
    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")
    public void deleteExpiredReadNotifications() {
        LocalDateTime now = LocalDateTime.now(SERVICE_ZONE);

        notificationService.deleteExpiredReadNotifications(now);
    }
}