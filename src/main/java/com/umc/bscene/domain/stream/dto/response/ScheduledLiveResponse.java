package com.umc.bscene.domain.stream.dto.response;

public record ScheduledLiveResponse(
        Long liveId,
        String bandProfileImageUrl,
        String title,
        String bandName,
        String scheduledAtText      // 예: "7.11. (금) 오후 9:00"
) {
}
