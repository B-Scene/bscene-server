package com.umc.bscene.domain.post.dto.response;

// 게시물 좋아요 등록/해제 응답 (변경 후 하트 상태와 좋아요 수)
public record PostLikeResponse(
        Long postId,
        boolean isLiked,
        long likeCount
) {

    public static PostLikeResponse of(Long postId, boolean isLiked, long likeCount) {
        return new PostLikeResponse(postId, isLiked, likeCount);
    }
}
