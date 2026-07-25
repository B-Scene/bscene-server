package com.umc.bscene.domain.stream.dto.response;

import java.util.List;

// 팬 모드 라이브 홈 응답
public record FanLiveHomeResponse(
        // 지금 라이브 중 3개. 시청자 수 실시간 갱신은 각 라이브의 GET /lives/{liveId}/viewers SSE 구독으로 처리
        List<LiveNowItem> liveNow,

        // 최신 다시보기 8개 (라이브별 대표 세그먼트, 업로드 최신순). 조회수는 스냅샷
        List<ReplayItem> replays,

        // 예정된 라이브 3개 (scheduledAt 오름차순)
        List<ScheduledItem> scheduled
) implements LiveHomeResponse {

    public record LiveNowItem(
            Long liveId,
            String bandProfileImageUrl,
            String title,
            String bandName,
            Integer viewerCount
    ) {
    }

    public record ReplayItem(
            Long replayId,
            String thumbnailImageUrl,   // 원본 라이브(AudioStream)의 썸네일
            String title,
            String bandName,
            Long viewCount
    ) {
    }

    public record ScheduledItem(
            Long liveId,
            String title,
            String bandName,
            String scheduledAt,
            Boolean notificationEnabled     // 나의 알림 설정 여부
    ) {
    }
}
