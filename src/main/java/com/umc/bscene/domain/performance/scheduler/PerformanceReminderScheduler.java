package com.umc.bscene.domain.performance.scheduler;

import com.umc.bscene.domain.performance.service.PerformanceReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.time.ZoneId;

@RequiredArgsConstructor
public class PerformanceReminderScheduler {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final PerformanceReminderService performanceReminderService;

    // 매분 정각에 공연 시작 1시간 전 알림 발송 대상을 확인
    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void sendUpcomingPerformanceReminders() {
        LocalDateTime now = LocalDateTime.now(SERVICE_ZONE);
        performanceReminderService.sendUpcomingReminders(now);
    }
}