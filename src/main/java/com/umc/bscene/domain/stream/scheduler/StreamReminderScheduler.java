package com.umc.bscene.domain.stream.scheduler;

import com.umc.bscene.domain.stream.service.StreamReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.time.ZoneId;

@RequiredArgsConstructor
public class StreamReminderScheduler {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final StreamReminderService streamReminderService;

    // 매분 정각에 예정 라이브 시작 30분 전 알림 발송 대상을 확인
    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void sendUpcomingStreamReminders() {
        LocalDateTime now = LocalDateTime.now(SERVICE_ZONE);
        streamReminderService.sendUpcomingReminders(now);
    }
}
