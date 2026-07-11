package com.umc.bscene.domain.fanhome.port;

import com.umc.bscene.domain.fanhome.dto.response.FanHomeResponse.HomePerformanceItem;

import java.util.List;

// 팬홈이 공연을 조회하기 위한 계약 (adapter는 performance 도메인이 구현)
public interface PerformancePort {

    // 아직 시작하지 않은 공연 중 관심 등록 수가 많은 순으로 limit개 반환 (추천 공연)
    List<HomePerformanceItem> recommendPerformances(int limit);

    /**
     * 주어진 밴드들의 아직 시작하지 않은 ACTIVE 공연을 시작 일시가 가까운 순으로 조회합니다.
     *
     * @param bandIds 공연을 조회할 밴드 ID 목록
     * @param limit   최대 조회 개수
     * @return 시작 일시가 가까운 순으로 정렬된 공연 목록
     */
    List<HomePerformanceItem> findUpcomingByBandIds(List<Long> bandIds, int limit);
}
