package com.umc.bscene.domain.session.scheduler;

import com.umc.bscene.domain.session.service.SessionRecruitmentReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.time.ZoneId;

@RequiredArgsConstructor
public class SessionRecruitmentReminderScheduler {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final SessionRecruitmentReminderService reminderService;

    // 매분 정각에 세션 모집 마감 24시간 전 알림 대상을 확인
    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void sendDeadlineReminders() {
        LocalDateTime now = LocalDateTime.now(SERVICE_ZONE);
        reminderService.sendDeadlineReminders(now);
    }
}