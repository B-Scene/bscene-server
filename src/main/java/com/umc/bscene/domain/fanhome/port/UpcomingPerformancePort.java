package com.umc.bscene.domain.fanhome.port;

import com.umc.bscene.domain.fanhome.dto.response.HomePerformanceItem;

import java.util.List;

/**
 * 팬홈이 팔로우한 밴드들의 다가오는 공연을 조회하기 위한 포트 (adapter는 performance 도메인이 구현).
 */
public interface UpcomingPerformancePort {
    /**
     * 주어진 밴드들의 아직 시작하지 않은 ACTIVE 공연을 시작 일시가 가까운 순으로 조회합니다.
     *
     * @param bandIds 공연을 조회할 밴드 ID 목록
     * @param limit   최대 조회 개수
     * @return 시작 일시가 가까운 순으로 정렬된 공연 목록
     */
    List<HomePerformanceItem> findUpcomingByBandIds(List<Long> bandIds, int limit);
}
