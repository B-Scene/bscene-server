package com.umc.bscene.domain.fanhome.port;

import com.umc.bscene.domain.fanhome.dto.response.RecommendedBandItem;

import java.util.List;

/**
 * 팬홈이 추천 밴드를 조회하기 위한 포트 (adapter는 band 도메인이 구현).
 */
public interface BandPort {
    /**
     * 사용자 맞춤 추천 밴드를 상위 개수만큼 조회합니다.
     *
     * @param userId 추천 대상 사용자 ID
     * @param limit  최대 조회 개수
     * @return 추천 점수가 높은 순으로 정렬된 추천 밴드 목록
     */
    List<RecommendedBandItem> recommendTopBands(Long userId, int limit);
}
