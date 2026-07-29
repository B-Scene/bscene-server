package com.umc.bscene.domain.stream.dto.response;

// 나의 알림 설정 여부(isAlarmSet)가 포함되는 유저별 응답이므로 캐싱하지 않는다
public record UpcomingLiveResponse(
        Long liveId,
        String bandProfileImageUrl,
        String title,
        String bandName,
        String scheduledAt,     // 예: "2026-07-11 21:00:00"
        Boolean isAlarmSet
) {
}
