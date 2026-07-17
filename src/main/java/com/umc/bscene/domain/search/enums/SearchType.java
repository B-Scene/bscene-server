package com.umc.bscene.domain.search.enums;

// 탐색 통합검색의 콘텐츠 필터
public enum SearchType {
    ALL,           // 통합 모드 : 밴드/공연/영상 섹션별 상위 4개
    BAND,          // 단일 모드 : 밴드만 무한스크롤
    PERFORMANCE,   // 단일 모드 : 공연만 무한스크롤
    VIDEO          // 단일 모드 : 영상만 무한스크롤
}
