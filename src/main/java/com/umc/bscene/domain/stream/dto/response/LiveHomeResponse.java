package com.umc.bscene.domain.stream.dto.response;

import com.umc.bscene.domain.user.enums.UserMode;

import java.util.List;

// 라이브 홈 통합 응답. currentMode에 따라 fanHome/bandHome 중 하나만 채워진다
public record LiveHomeResponse(
        UserMode mode,
        FanHome fanHome,        // mode == FAN 일 때만 채움, 아니면 null
        BandHome bandHome       // mode == BAND 일 때만 채움, 아니면 null
) {

    public record FanHome(
            // 지금 라이브 중 3개. 시청자 수 실시간 갱신은 각 라이브의 GET /lives/{liveId}/viewers SSE 구독으로 처리
            List<LiveStreamResponse> liveNow,

            /*
             * TODO: [다시보기 8개 섹션 - 구현 보류]
             * 다시보기(Replay) 테이블 생성 후 최신 다시보기 8개 목록 필드 추가.
             * 원본 브랜치의 라이브 종료 후 다시보기 업로드 로직 완성 전까지 구현하지 않는다.
             */

            // 예정된 라이브 3개 (scheduledAt 오름차순)
            List<ScheduledLiveResponse> scheduledLives
    ) {
    }

    public record BandHome(
            // 지금 라이브 중 3개
            List<BandLiveNowItem> liveNow,

            // 내가 예약한 라이브 5개 (scheduledAt 오름차순)
            List<ScheduledLiveResponse> myScheduledLives
    ) {
    }

    // 밴드모드 라이브 중 row. isMyLive=true면 프론트가 "내 라이브 진행중" 라벨을, false면 bandName을 노출
    public record BandLiveNowItem(
            Long liveId,
            Boolean isMyLive,
            String bandName,
            String title,
            Integer viewCount
    ) {
    }
}
