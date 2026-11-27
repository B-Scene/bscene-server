package com.umc.bscene.domain.stream.scheduler;

import com.umc.bscene.domain.stream.service.ReplayExpirationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class ReplayExpirationScheduler {

    private final ReplayExpirationService replayExpirationService;

    // 1시간마다 다음 1시간 안에 만료되는 다시보기를 예약.
    // fixedRate는 기동 직후에도 한 번 실행되므로 재시작으로 메모리에서 사라진 예약을 바로 복구한다
    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS, scheduler = "replayExpirationTaskScheduler")
    public void reserveExpiringReplays() {
        replayExpirationService.reserveExpiringReplays();
    }
}
