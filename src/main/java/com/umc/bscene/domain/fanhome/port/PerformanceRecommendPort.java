package com.umc.bscene.domain.fanhome.port;

import com.umc.bscene.domain.fanhome.dto.response.HomePerformanceItem;

import java.util.List;

// 팬홈이 추천 공연을 조회하기 위한 계약 (adapter는 performance 도메인이 구현)
// TODO: 지금은 임시 스텁(ACTIVE 공연 최신 N개), 추후 공연 추천 알고리즘으로 교체
public interface PerformanceRecommendPort {

    // 사용자에게 추천할 공연을 limit개 반환
    List<HomePerformanceItem> recommendPerformances(Long userId, int limit);
}
