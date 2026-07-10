package com.umc.bscene.domain.stream.dto.response;

import java.util.List;

public record MtxPathListResponse(
        Integer itemCount,
        Integer pageCount,
        List<Item> items
) {
    public record Item(String name, Boolean ready) {
    }
}
