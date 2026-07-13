package com.umc.bscene.domain.stream.dto.response;

public record StreamReplayResponse(
        String title,
        String bandName,
        String bandProfileImageUrl,
        long viewCount,
        int durationSec,
        String playbackUrl
) {
}
