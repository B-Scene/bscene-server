package com.umc.bscene.domain.fanhome.dto.response;

import java.time.LocalDate;
import java.util.List;

// 공연 달력 응답 : 해당 년월에 공연이 있는 날짜 목록(점 표시용) + 오늘 날짜
public record PerformanceCalendarResponse(
        int year,
        int month,
        LocalDate today,
        List<LocalDate> performanceDates
) {
}
