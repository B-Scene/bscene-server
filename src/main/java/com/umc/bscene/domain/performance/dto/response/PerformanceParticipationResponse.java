package com.umc.bscene.domain.performance.dto.response;

import com.umc.bscene.domain.performance.enums.ParticipationStatus;

public record PerformanceParticipationResponse(
        Long performanceId,
        ParticipationStatus status
) {
    public static PerformanceParticipationResponse of(Long performanceId, ParticipationStatus status) {
        return new PerformanceParticipationResponse(performanceId, status);
    }
}
