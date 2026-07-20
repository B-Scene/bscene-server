package com.umc.bscene.domain.search.enums;

// 탐색 통합검색의 정렬 필터 (마지막 tie-breaker docId는 공통 — PK 내림차순이라 최신순 겸용)
public enum SearchSortType {
    ACCURACY,   // 정확도순 : _score → 날짜 → docId
    POPULAR     // 인기순 : popularity(밴드=팔로워수, 공연=관심수, 게시물=밴드 팔로워수) → _score → docId
}
