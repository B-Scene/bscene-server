package com.umc.bscene.domain.performance.dto.response;

import java.util.List;

public record PerformanceListResponse(
        List<PerformanceSummaryResponse> performances
) {
    public static PerformanceListResponse from(List<PerformanceSummaryResponse> performances) {
        return new PerformanceListResponse(performances);
    }
}
