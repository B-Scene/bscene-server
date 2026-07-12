package com.umc.bscene.domain.fanhome.dto.response;

import java.time.LocalDate;
import java.util.List;

// 특정 날짜 공연 목록 응답 (offset 기반 무한스크롤, 시간 빠른 순)
public record DatePerformanceResponse(
        LocalDate date,    // 조회된 날짜 (요청에서 생략 시 서버 기준 오늘)
        List<PerformanceListItem> items,
        int page,          // 현재 페이지 (0-base)
        boolean hasNext    // 다음 페이지 존재 여부
) {
}
