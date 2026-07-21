package com.umc.bscene.domain.stream.dto.response;

public record LiveStreamResponse(
        Long liveId,
        String bandProfileImageUrl,
        String title,
        String bandName,
        Integer viewCount
) { }
