package com.umc.bscene.domain.recommendation.service;

import com.umc.bscene.domain.recommendation.dto.response.BandInteractionResponse;
import com.umc.bscene.domain.recommendation.repository.BandInteractionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    // 사용자의 클릭 수 상위 N개 밴드 조회
    public List<BandInteractionResponse> getTopInteractedBands(Long userId, int limit) {
        return bandInteractionRepository.findByUser_IdOrderByClickCountDesc(userId, PageRequest.of(0, limit)).stream()
                .map(BandInteractionResponse::from)
                .toList();
    }

    // 특정 유저-밴드 쌍의 상호작용 단건 조회
    public Optional<BandInteractionResponse> getInteraction(Long userId, Long bandId) {
        return bandInteractionRepository.findByUser_IdAndBand_Id(userId, bandId)
                .map(BandInteractionResponse::from);
    }
}
