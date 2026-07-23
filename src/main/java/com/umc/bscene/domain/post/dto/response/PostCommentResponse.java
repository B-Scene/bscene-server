package com.umc.bscene.domain.post.dto.response;

import com.umc.bscene.domain.post.entity.PostComment;

import java.time.LocalDateTime;

// 게시물 댓글 작성/수정/삭제 응답
public record PostCommentResponse(
        Long commentId,
        String content,                     // 삭제 응답에서는 null
        LocalDateTime createdAt             // 삭제 응답에서는 null
) {

    public static PostCommentResponse from(PostComment comment) {
        return new PostCommentResponse(comment.getId(), comment.getContent(), comment.getCreatedAt());
    }

    public static PostCommentResponse deleted(Long commentId) {
        return new PostCommentResponse(commentId, null, null);
    }
}
