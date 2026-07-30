package com.umc.bscene.domain.stream.dto.response;

import java.util.List;

// 밴드 모드 라이브 홈 응답
public record BandLiveHomeResponse(
        Long userId,
        // 지금 라이브 중 3개. isMine=true면 프론트가 "내 라이브 진행중" 라벨 노출
        List<LiveNowItem> liveNow,

        // 예정된 라이브 5개 (scheduledAt 오름차순, isMine으로 내 예약 구분)
        List<ScheduledItem> scheduled
) implements LiveHomeResponse {

    public record LiveNowItem(
            Long liveId,
            String bandProfileImageUrl,
            String bandName,
            String title,
            Integer viewerCount,
            Boolean isMine,

            // 진행자(송출자 + 공동 진행자)로 등록된 유저 ID 목록.
            // 조회자가 이 목록에 없으면 프론트가 공동 진행자 업그레이드 요청 모달을 노출
            List<Long> coHost
    ) {
    }

    public record ScheduledItem(
            Long liveId,
            String bandName,
            String title,
            String scheduledAt,
            Boolean isMine,

            // 진행자(송출자 + 공동 진행자)로 등록된 유저 ID 목록
            List<Long> coHost
    ) {
    }
}
