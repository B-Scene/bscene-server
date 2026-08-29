package com.umc.bscene.domain.band.scheduler;

import com.umc.bscene.domain.band.entity.BandCreationRequest;
import com.umc.bscene.domain.band.repository.BandCreationRequestRepository;
import com.umc.bscene.domain.band.service.BandVerifyMessenger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Discord 검수 메시지 전송 실패 재시도 안전망.
 * 전송에 실패하면 요청이 discordMessageId 없이 PENDING으로 남아 운영진이 처리할 수 없고
 * 그 밴드명은 계속 점유되므로, 미전송 요청을 주기적으로 재전송한다.
 * 봇 미설정 환경에서는 빈 자체가 등록되지 않는다 (DiscordBotConfig와 동일 조건).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnExpression("!'${discord.bot.token:}'.isEmpty()")
public class BandVerifyRetryScheduler {

    // 이 기간을 넘긴 미전송 요청은 자동 재시도에서 제외 - 채널 설정 오류 같은 영구 실패의 무한 재시도 방지 (수동 확인 필요)
    private static final Duration RETRY_WINDOW = Duration.ofDays(3);

    private final BandCreationRequestRepository bandCreationRequestRepository;
    private final BandVerifyMessenger bandVerifyMessenger;

    @Scheduled(initialDelay = 60_000, fixedDelay = 300_000)
    public void resendUnsentVerifyMessages() {
        List<BandCreationRequest> unsentRequests = bandCreationRequestRepository
                .findAllByResolvedAtIsNullAndDiscordMessageIdIsNull();
        if (unsentRequests.isEmpty()) {
            return;
        }

        LocalDateTime retryLimit = LocalDateTime.now().minus(RETRY_WINDOW);
        int expiredCount = 0;

        for (BandCreationRequest request : unsentRequests) {
            if (request.getCreatedAt() != null && request.getCreatedAt().isBefore(retryLimit)) {
                expiredCount++;
                continue;
            }
            // sendVerifyMessage는 건별 비동기(@Async) + 예외 내부 처리 + 조건부 저장이라 한 건의 실패가 다음 건을 막지 않는다
            bandVerifyMessenger.sendVerifyMessage(request.getId());
        }

        if (expiredCount > 0) {
            log.warn("재시도 기한({}일)을 넘긴 미전송 검수 요청 {}건 - 수동 확인 필요",
                    RETRY_WINDOW.toDays(), expiredCount);
        }
    }
}
