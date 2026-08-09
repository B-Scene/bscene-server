package com.umc.bscene.domain.fanhome.dto.response;

import com.umc.bscene.domain.fanhome.enums.RecommendSortType;

import java.util.List;

// 추천 공연 전체 목록 응답 (offset 기반 무한스크롤)
// 팔로우 여부와 무관하게 전체 공연을 대상으로 한다 — 홈 공연 섹션이 RECOMMENDED일 때의 더보기 화면
public record RecommendedPerformanceResponse(
        RecommendSortType sort,            // 적용된 정렬 기준 (POPULAR/IMMINENT)
        List<PerformanceListItem> items,
        int page,          // 현재 페이지 (0-base)
        boolean hasNext    // 다음 페이지 존재 여부
) {
}
