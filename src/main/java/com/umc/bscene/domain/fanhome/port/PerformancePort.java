package com.umc.bscene.domain.fanhome.port;

import com.umc.bscene.domain.fanhome.dto.response.FanHomeResponse.HomePerformanceItem;

import java.util.List;

// 팬홈이 추천 공연을 조회하기 위한 계약 (adapter는 performance 도메인이 구현)
// TODO: 지금은 임시 스텁(ACTIVE 공연 최신 N개), 추후 공연 추천 알고리즘으로 교체
public interface PerformancePort {

    // 사용자에게 추천할 공연을 limit개 반환
    List<HomePerformanceItem> recommendPerformances(Long userId, int limit);

    /**
     * 주어진 밴드들의 아직 시작하지 않은 ACTIVE 공연을 시작 일시가 가까운 순으로 조회합니다.
     *
     * @param bandIds 공연을 조회할 밴드 ID 목록
     * @param limit   최대 조회 개수
     * @return 시작 일시가 가까운 순으로 정렬된 공연 목록
     */
    List<HomePerformanceItem> findUpcomingByBandIds(List<Long> bandIds, int limit);
}
