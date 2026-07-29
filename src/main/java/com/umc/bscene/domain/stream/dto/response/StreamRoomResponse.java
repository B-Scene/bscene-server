package com.umc.bscene.domain.stream.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record StreamRoomResponse(
        Long liveId,
        Boolean isLive,

        // 소수점 아래 삭제
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime startedAt,

        Integer viewCount,
        String bandProfileImageUrl,
        String bandName,
        String title,
        String description,
        Playback playback
) {

    public record Playback(
            String role,        // BROADCASTER / LISTENER
            String protocol,    // WHIP / HLS
            String playbackUrl
    ) {}
}
