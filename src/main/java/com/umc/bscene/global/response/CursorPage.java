package com.umc.bscene.global.response;

import java.util.List;

public class CursorPage<T> {

    private List<T> items;
    private PageInfo pageInfo;

    private CursorPage(List<T> items, PageInfo pageInfo) {
        this.items = items;
        this.pageInfo = pageInfo;
    }

    record PageInfo(
            Long nextCursor,
            Boolean hasNext
    ) {}

    public static <T> CursorPage<T> of(List<T> items, Long nextCursor, Boolean hasNext) {
        return new CursorPage<>(items, new PageInfo(nextCursor, hasNext));
    }

    public static <T> CursorPage<T> ofHasNext(List<T> items, Long nextCursor) {
        return new CursorPage<>(items, new PageInfo(nextCursor, true));
    }

    public static <T> CursorPage<T> ofLastPage(List<T> items) {
        return new CursorPage<>(items, new PageInfo(null, false));
    }
}
