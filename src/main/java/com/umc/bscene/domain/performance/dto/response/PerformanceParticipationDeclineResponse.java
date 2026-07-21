package com.umc.bscene.domain.performance.dto.response;

// 공연 불참 처리 응답
public record PerformanceParticipationDeclineResponse(
        Long performanceId
) {
    public static PerformanceParticipationDeclineResponse of(Long performanceId) {
        return new PerformanceParticipationDeclineResponse(performanceId);
    }
}
