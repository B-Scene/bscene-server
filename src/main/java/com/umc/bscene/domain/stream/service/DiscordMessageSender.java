package com.umc.bscene.domain.stream.service;

import com.umc.bscene.domain.stream.dto.request.DiscordWebhookRequest;
import com.umc.bscene.domain.stream.entity.mapper.ReportHistory;
import com.umc.bscene.domain.stream.enums.DiscordEventMessage;
import com.umc.bscene.domain.stream.port.DiscordWebhookPort;
import com.umc.bscene.domain.stream.repository.ReportHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
public class DiscordMessageSender {

    private final DiscordWebhookPort discordWebhookPort;
    private final ReportHistoryRepository reportHistoryRepository;

    // 신고 커밋 직후의 즉시 발송 경로. 실패해도 예외를 전파하지 않는다
    // - 미발송(discordNotifiedAt = null) 건은 DiscordNotifyRetryScheduler가 재발송한다
    @Async
    public void sendReportNotificationAsync(Long reportHistoryId) {
        reportHistoryRepository.findById(reportHistoryId).ifPresent(reportHistory -> {
            try {
                sendReportNotification(reportHistory);
            } catch (Exception e) {
                log.warn("디스코드 신고 알림 즉시 발송 실패. 재발송 스케줄러가 처리한다. reportHistoryId={}", reportHistoryId, e);
            }
        });
    }

    // 발송 성공 시에만 발송 완료로 마킹한다. 실패 시 예외가 전파되므로 호출부가 재시도 정책을 결정한다
    public void sendReportNotification(ReportHistory reportHistory) {
        discordWebhookPort.send(DiscordWebhookRequest.of(
                DiscordEventMessage.USER_REPORT_EVENT,
                reportHistory.getReporterId(),
                reportHistory.getTargetUser().getId(),
                reportHistory.getReportType(),
                reportHistory.getChatMessage(),
                reportHistory.getComment()
        ));

        reportHistoryRepository.markDiscordNotified(reportHistory.getId(), LocalDateTime.now());
    }
}
