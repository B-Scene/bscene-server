package com.umc.bscene.domain.stream.dto.response;

// 라이브 푸시 알림 발송 시 송출자의 소속 밴드를 식별하기 위한 최소 정보
public record BandSummaryResponse(
        Long bandId,
        String bandName
) {
}
