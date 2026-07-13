package com.umc.bscene.global.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Getter
public class CursorPage<T> {

    private List<T> items;
    private PageInfo pageInfo;

    // Redis 캐시(JSON) 역직렬화에 사용할 생성자 지정
    @JsonCreator
    private CursorPage(
            @JsonProperty("items") List<T> items,
            @JsonProperty("pageInfo") PageInfo pageInfo
    ) {
        this.items = items;
        this.pageInfo = pageInfo;
    }

    public record PageInfo(
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

    public static <T> CursorPage<T> empty() {
        return new CursorPage<>(Collections.emptyList(), new PageInfo(null, false));
    }
}
