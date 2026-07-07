package com.umc.bscene.domain.post.dto.response;

import java.util.List;

public record PostListResponse(
        List<PostSummaryResponse> posts,
        boolean hasNext,
        Long nextCursor
) {
    public static PostListResponse of(List<PostSummaryResponse> posts, boolean hasNext, Long nextCursor) {
        return new PostListResponse(posts, hasNext, nextCursor);
    }
}
