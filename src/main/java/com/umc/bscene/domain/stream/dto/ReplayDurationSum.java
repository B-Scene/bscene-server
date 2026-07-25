package com.umc.bscene.domain.stream.dto;

/** 다시보기 목록의 라이브별 총 재생 길이 프로젝션 (세그먼트 durationSec 합산) */
public record ReplayDurationSum(
        Long audioStreamId,
        Long totalDurationSec
) {
}
