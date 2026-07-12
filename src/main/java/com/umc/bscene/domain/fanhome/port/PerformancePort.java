package com.umc.bscene.domain.fanhome.port;

import com.umc.bscene.domain.fanhome.dto.response.FanHomeResponse.HomePerformanceItem;
import com.umc.bscene.domain.fanhome.dto.response.UpcomingPerformanceResponse;
import com.umc.bscene.domain.fanhome.enums.UpcomingSortType;

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

    /**
     * 팔로우한 밴드들의 아직 시작하지 않은 ACTIVE 공연을 정렬 기준에 따라 페이지 단위로 조회합니다. (다가오는 공연 전체 목록)
     *
     * @param bandIds 공연을 조회할 밴드 ID 목록
     * @param sort    정렬 기준 (임박순/최신순/인기순)
     * @param page    페이지 번호 (0-base)
     * @param size    페이지 크기
     */
    UpcomingPerformanceResponse findUpcoming(List<Long> bandIds, UpcomingSortType sort, int page, int size);
}
