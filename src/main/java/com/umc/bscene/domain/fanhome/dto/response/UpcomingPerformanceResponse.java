package com.umc.bscene.domain.fanhome.dto.response;

import com.umc.bscene.domain.fanhome.enums.UpcomingSortType;

import java.util.List;

// 다가오는 공연 전체 목록 응답 (offset 기반 무한스크롤)
public record UpcomingPerformanceResponse(
        UpcomingSortType sort,             // 적용된 정렬 기준 (IMMINENT/LATEST/POPULAR)
        List<PerformanceListItem> items,
        int page,          // 현재 페이지 (0-base)
        boolean hasNext    // 다음 페이지 존재 여부
) {
}
