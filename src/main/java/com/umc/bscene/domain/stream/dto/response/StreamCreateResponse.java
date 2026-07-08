package com.umc.bscene.domain.stream.dto.response;

public record StreamCreateResponse(
        Long audioStreamId,
        String path,
        String title
) {
}
