package com.umc.bscene.domain.recommendation.event;

import com.umc.bscene.domain.band.repository.BandRepository;
import com.umc.bscene.domain.recommendation.entity.BandRecommendationLog;
import com.umc.bscene.domain.recommendation.repository.BandRecommendationLogRepository;
import com.umc.bscene.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * 추천 노출 이벤트를 받아 BandRecommendationLog를 비동기로 적재하는 리스너.
 * - AFTER_COMMIT : 추천 응답 트랜잭션이 커밋된 뒤에만 로그를 남긴다.
 * - @Async : 로그 적재가 추천 응답 시간을 지연시키지 않는다.
 * - 로그 저장 실패는 로그만 남기고 삼킨다 → 추천 응답 자체에는 영향을 주지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationLogEventListener {

    private final BandRecommendationLogRepository bandRecommendationLogRepository;
    private final UserRepository userRepository;
    private final BandRepository bandRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBandRecommendationExposed(BandRecommendationExposedEvent event) {
        try {
            List<BandRecommendationLog> logs = event.exposures().stream()
                    .map(exposure -> BandRecommendationLog.builder()
                            .user(userRepository.getReferenceById(event.userId()))
                            .band(bandRepository.getReferenceById(exposure.bandId()))
                            .position(exposure.position())
                            .algorithmVersion(exposure.algorithmVersion())
                            .build())
                    .toList();
            bandRecommendationLogRepository.saveAll(logs);
        } catch (RuntimeException e) {
            log.error("추천 노출 로그 저장 실패 - userId: {}", event.userId(), e);
        }
    }
}
