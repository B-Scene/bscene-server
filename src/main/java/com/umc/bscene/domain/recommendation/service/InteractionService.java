package com.umc.bscene.domain.recommendation.service;

import com.umc.bscene.domain.recommendation.repository.BandInteractionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InteractionService {

    private static final long CLICK_DEDUP_WINDOW_MINUTES = 10;

    private final BandInteractionRepository bandInteractionRepository;

    // 사용자-밴드 클릭 상호작용 기록 (SELECT 없이 upsert 네이티브 쿼리로 원자 처리, 중복 제거 기간 내 재조회는 무시)
    @Transactional
    public void recordClick(Long userId, Long bandId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dedupBefore = now.minusMinutes(CLICK_DEDUP_WINDOW_MINUTES);
        bandInteractionRepository.upsertInteraction(userId, bandId, now, dedupBefore, now);
    }
}
