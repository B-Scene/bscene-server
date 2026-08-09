package com.umc.bscene.domain.fanhome.enums;

// 추천 공연 목록 정렬 기준 (팔로우 밴드의 다가오는 공연이 없을 때 홈 더보기 화면)
public enum RecommendSortType {
    POPULAR,    // 인기순 (관심 등록 수 많은 순) — 기본값
    IMMINENT    // 공연임박순 (날짜·시각 가까운 순)
}
