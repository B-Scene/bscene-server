package com.umc.bscene.domain.fanhome.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

// 공연 목록 카드 한 개 (다가오는 공연 목록 / 날짜별 목록 공용)
public record PerformanceListItem(
        Long performanceId,
        String title,
        String venue,
        LocalDate performanceDate,
        LocalTime startTime,
        String posterImageUrl,
        boolean isInterested    // 로그인 유저의 관심 등록 여부
) {
}
