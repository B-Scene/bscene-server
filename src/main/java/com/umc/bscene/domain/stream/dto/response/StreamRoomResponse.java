package com.umc.bscene.domain.stream.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

public record StreamRoomResponse(
        Long liveId,
        Boolean isLive,

        String nickname,

        // 소수점 아래 삭제
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime startedAt,

        Integer viewCount,
        String bandProfileImageUrl,
        String bandName,
        String title,
        String description,
        Playback playback,

        // 진행자 전용: 본인을 제외한 다른 진행자들의 WHEP 모니터링 정보.
        // 멤버 path가 노출되므로 청취자 응답에서는 반드시 null (canRead가 2차 방어)
        List<CoPublisher> coPublishers
) {

    public record Playback(
            String role,        // BROADCASTER / CO_HOST / LISTENER
            String protocol,    // WHIP / HLS
            String playbackUrl
    ) {}

    public record CoPublisher(
            Long userId,
            String whepUrl
    ) {}
}
