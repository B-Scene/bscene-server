package com.umc.bscene.domain.stream.dto;

import java.time.LocalDateTime;

/** 다시보기 만료 판정용 라이브별 첫 게시 시각 프로젝션 (세그먼트 createdAt 중 최솟값) */
public record ReplayFirstPostedAt(
        Long audioStreamId,
        LocalDateTime firstPostedAt
) {
}
