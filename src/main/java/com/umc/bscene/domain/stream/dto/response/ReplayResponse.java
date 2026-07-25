package com.umc.bscene.domain.stream.dto.response;

// 다시보기 목록 조회 row. 조회수는 스냅샷 (실시간 갱신 없음)
public record ReplayResponse(
        Long replayId,
        String thumbnailImageUrl,   // 원본 라이브(AudioStream)의 썸네일
        String title,
        String bandName,
        Long viewCount,
        Integer durationSec     // 라이브 전체 재생 길이(초). 세그먼트가 여러 개면 합산 (watchReplay와 동일 기준)
) {
}
