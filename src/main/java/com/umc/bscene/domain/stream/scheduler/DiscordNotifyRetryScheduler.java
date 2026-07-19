package com.umc.bscene.domain.stream.scheduler;

import com.umc.bscene.domain.stream.entity.mapper.ReportHistory;
import com.umc.bscene.domain.stream.repository.ReportHistoryRepository;
import com.umc.bscene.domain.stream.service.DiscordMessageSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

// 디스코드 장기 장애 등으로 즉시 발송에 실패한 신고 알림을 재발송하는 안전망 스케줄러
@Slf4j
@RequiredArgsConstructor
public class DiscordNotifyRetryScheduler {

    // 이 시간 안의 신고 건은 즉시 발송 경로가 아직 처리 중일 수 있으므로 스캔에서 제외해 이중 발송을 피한다
    private static final Duration IMMEDIATE_SEND_GRACE = Duration.ofMinutes(5);

    // 이 기간이 지난 미발송 건은 재발송을 포기한다 (신고 이력 자체는 DB에 남아 있으므로 유실은 아니다)
    private static final Duration RESEND_EXPIRE = Duration.ofDays(7);

    private final ReportHistoryRepository reportHistoryRepository;
    private final DiscordMessageSender discordMessageSender;

    @Scheduled(fixedDelay = 300_000)
    public void resendUnnotifiedReports() {
        LocalDateTime now = LocalDateTime.now();

        // 디스코드 웹훅 rate limit 대비 한 주기 최대 20건 - 남은 건은 다음 주기에 이어서 처리된다
        List<ReportHistory> targets = reportHistoryRepository
                .findTop20ByDiscordNotifiedAtIsNullAndCreatedAtBetweenOrderByCreatedAtAsc(
                        now.minus(RESEND_EXPIRE),
                        now.minus(IMMEDIATE_SEND_GRACE)
                );

        for (ReportHistory reportHistory : targets) {
            try {
                discordMessageSender.sendReportNotification(reportHistory);
            } catch (Exception e) {
                // 디스코드 장애가 지속 중이면 나머지도 실패할 것이므로 이번 주기는 여기서 중단한다
                log.warn("디스코드 신고 알림 재발송 실패. 다음 주기에 재시도한다. reportHistoryId={}", reportHistory.getId(), e);
                return;
            }
        }
    }
}
