package com.umc.bscene.domain.performance.dto.response;

import com.umc.bscene.domain.performance.entity.Performance;

import java.time.LocalDate;

public record PerformanceSummaryResponse(
        Long performanceId,
        String title,
        LocalDate performanceDate,
        String venue,
        String posterImageUrl
) {
    public static PerformanceSummaryResponse from(Performance performance) {
        return new PerformanceSummaryResponse(
                performance.getId(),
                performance.getTitle(),
                performance.getPerformanceDate(),
                performance.getVenue(),
                performance.getPosterImageUrl()
        );
    }
}
