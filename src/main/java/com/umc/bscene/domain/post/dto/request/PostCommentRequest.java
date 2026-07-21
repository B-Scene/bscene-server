package com.umc.bscene.domain.post.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 게시물 댓글 작성/수정 요청
public record PostCommentRequest(

        @NotBlank(message = "댓글 내용을 입력해주세요.")
        @Size(max = 500, message = "댓글은 500자 이내여야 합니다.")
        String content
) {
}
