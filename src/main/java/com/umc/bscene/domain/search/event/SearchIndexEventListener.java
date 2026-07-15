package com.umc.bscene.domain.search.event;

import com.umc.bscene.domain.search.service.SearchIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 도메인 변경 이벤트를 받아 검색 색인을 동기화하는 리스너.
 * - AFTER_COMMIT : MySQL 커밋이 확정된 뒤에만 색인 (롤백된 변경이 ES에 새는 것 방지)
 * - @Async : 색인이 원 요청의 응답 시간을 지연시키지 않음 (ES 장애가 본 기능에 전파되지 않음)
 * - 색인 실패는 로그만 남긴다 → 새벽 전체 재색인 스케줄러가 복구하는 안전망 구조
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchIndexEventListener {

    private final SearchIndexService searchIndexService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBandChanged(BandChangedEvent event) {
        try {
            searchIndexService.indexBand(event.bandId());
        } catch (RuntimeException e) {
            log.error("밴드 색인 동기화 실패 - bandId: {}", event.bandId(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePerformanceChanged(PerformanceChangedEvent event) {
        try {
            searchIndexService.indexPerformance(event.performanceId());
        } catch (RuntimeException e) {
            log.error("공연 색인 동기화 실패 - performanceId: {}", event.performanceId(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePostChanged(PostChangedEvent event) {
        try {
            searchIndexService.indexVideo(event.postId());
        } catch (RuntimeException e) {
            log.error("영상 색인 동기화 실패 - postId: {}", event.postId(), e);
        }
    }
}
