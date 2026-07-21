package com.umc.bscene.domain.performance.dto.response;

public record PerformanceInterestResponse(
        Long performanceId,
        boolean isInterested
) {
    public static PerformanceInterestResponse of(Long performanceId, boolean interested) {
        return new PerformanceInterestResponse(performanceId, interested);
    }
}
