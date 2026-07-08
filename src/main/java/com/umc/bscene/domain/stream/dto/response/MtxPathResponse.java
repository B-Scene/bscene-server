package com.umc.bscene.domain.stream.dto.response;

public record MtxPathResponse (
        Source source
) {
    public record Source(String type, String id) {}
}
