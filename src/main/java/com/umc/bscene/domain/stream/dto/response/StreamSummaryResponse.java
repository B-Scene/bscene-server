package com.umc.bscene.domain.stream.dto.response;

public record StreamSummaryResponse(
        String title,
        int durationSec,
        int closedViewerCount
) {}
