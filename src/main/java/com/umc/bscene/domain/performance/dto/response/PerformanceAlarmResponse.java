package com.umc.bscene.domain.performance.dto.response;

public record PerformanceAlarmResponse(
        Long performanceId,
        boolean isAlarmSet
) {
    public static PerformanceAlarmResponse of(Long performanceId, boolean alarmEnabled) {
        return new PerformanceAlarmResponse(performanceId, alarmEnabled);
    }
}
