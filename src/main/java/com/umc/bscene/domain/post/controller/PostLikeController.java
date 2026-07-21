package com.umc.bscene.domain.post.controller;

import com.umc.bscene.domain.post.dto.response.PostLikeResponse;
import com.umc.bscene.domain.post.response.code.PostSuccessCode;
import com.umc.bscene.domain.post.service.PostLikeService;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/posts/{postId}/likes")
public class PostLikeController {

    private final PostLikeService postLikeService;

    // 게시물 좋아요 등록 API
    @PostMapping
    public ResponseEntity<SuccessResponse<PostLikeResponse>> setLike(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long postId
    ) {
        PostLikeResponse response = postLikeService.setLike(authMember.getUser().getId(), postId);
        SuccessResponse<PostLikeResponse> successResponse = SuccessResponse.of(
                response,
                PostSuccessCode.POST_LIKE_SET_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 게시물 좋아요 해제 API (좋아요하지 않은 상태여도 멱등 처리)
    @DeleteMapping
    public ResponseEntity<SuccessResponse<PostLikeResponse>> unsetLike(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long postId
    ) {
        PostLikeResponse response = postLikeService.unsetLike(authMember.getUser().getId(), postId);
        SuccessResponse<PostLikeResponse> successResponse = SuccessResponse.of(
                response,
                PostSuccessCode.POST_LIKE_UNSET_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }
}
