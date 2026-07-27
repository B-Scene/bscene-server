package com.umc.bscene.domain.stream.dto.response;

// 다시보기 목록 조회 row. 조회수는 스냅샷 (실시간 갱신 없음)
public record ReplayResponse(
        Long liveId,
        String thumbnailUrl,
        String title,
        String bandName,
        Long viewCount
) {
}
