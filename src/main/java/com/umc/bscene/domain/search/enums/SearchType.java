package com.umc.bscene.domain.search.enums;

// 탐색 통합검색의 콘텐츠 필터
public enum SearchType {
    ALL,           // 통합 모드 : 밴드/공연/게시물 섹션별 상위 3개
    BAND,          // 단일 모드 : 밴드만 무한스크롤
    PERFORMANCE,   // 단일 모드 : 공연만 무한스크롤
    POST           // 단일 모드 : 게시물(영상/사진/글)만 무한스크롤
}
