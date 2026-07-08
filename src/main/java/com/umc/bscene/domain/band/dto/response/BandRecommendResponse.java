package com.umc.bscene.domain.band.dto.response;

import java.util.List;

public record BandRecommendResponse(
        List<BandRecommendItem> bands,
        boolean hasNext,
        Long nextCursor,
        boolean fallback
) {
    public static BandRecommendResponse of(List<BandRecommendItem> bands, boolean hasNext, Long nextCursor) {
        return new BandRecommendResponse(bands, hasNext, nextCursor, false);
    }
}
