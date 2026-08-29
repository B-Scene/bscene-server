package com.umc.bscene.domain.band.dto;

// Discord 검수(실존 확인) 채널로 보낼 밴드 생성 요청 정보
public record BandVerifyMessage(
        Long requestId,
        String bandName,
        String genre,
        String region,
        String description,
        String profileImageUrl
) {
}
