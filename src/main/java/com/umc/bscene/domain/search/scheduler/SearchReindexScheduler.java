package com.umc.bscene.domain.search.scheduler;

import com.umc.bscene.domain.search.service.SearchIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 검색 색인 정기 전체 재색인 (안전망).
 * 이벤트 기반 동기화가 유실됐을 때(색인 실패, 발행 전 서버 재시작 등)
 * 하루 안에 원본(MySQL)과 색인이 다시 일치하도록 보장한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchReindexScheduler {

    private final SearchIndexService searchIndexService;

    // 트래픽 최저 시간대에 실행 (재색인 중 몇 초간 검색 결과가 비는 트레이드오프)
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void reindexAll() {
        try {
            searchIndexService.reindexAll();
        } catch (RuntimeException e) {
            log.error("정기 전체 재색인 실패", e);
        }
    }
}
