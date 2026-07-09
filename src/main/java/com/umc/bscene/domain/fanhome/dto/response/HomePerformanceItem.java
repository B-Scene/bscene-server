package com.umc.bscene.domain.fanhome.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

// 공연 카드 한 개 (다가오는 공연 / 추천 공연 공통)
public record HomePerformanceItem(
        Long performanceId,
        String title,
        String venue,
        LocalDate performanceDate,
        LocalTime startTime,
        String posterImageUrl     // D-day는 프론트가 계산
) {
}
