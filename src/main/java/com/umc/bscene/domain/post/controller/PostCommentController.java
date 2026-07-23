package com.umc.bscene.domain.post.controller;

import com.umc.bscene.domain.post.dto.request.PostCommentRequest;
import com.umc.bscene.domain.post.dto.response.PostCommentListResponse;
import com.umc.bscene.domain.post.dto.response.PostCommentResponse;
import com.umc.bscene.domain.post.response.code.PostSuccessCode;
import com.umc.bscene.domain.post.service.PostCommentService;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/posts/{postId}/comments")
public class PostCommentController {

    private final PostCommentService postCommentService;

    // 게시물 댓글 목록 조회 API (등록순, 커서 기반 무한스크롤 — 첫 페이지에는 내 댓글이 myComments로 분리되어 포함)
    @GetMapping
    public ResponseEntity<SuccessResponse<PostCommentListResponse>> getComments(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long postId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer size
    ) {
        PostCommentListResponse response = postCommentService.getComments(
                authMember.getUser().getId(), postId, cursor, size);
        SuccessResponse<PostCommentListResponse> successResponse = SuccessResponse.of(
                response,
                PostSuccessCode.POST_COMMENT_LIST_GET_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 게시물 댓글 작성 API
    @PostMapping
    public ResponseEntity<SuccessResponse<PostCommentResponse>> createComment(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long postId,
            @Valid @RequestBody PostCommentRequest request
    ) {
        PostCommentResponse response = postCommentService.createComment(
                authMember.getUser().getId(), postId, request);
        SuccessResponse<PostCommentResponse> successResponse = SuccessResponse.of(
                response,
                PostSuccessCode.POST_COMMENT_CREATE_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 게시물 댓글 수정 API (본인 댓글만)
    @PatchMapping("/{commentId}")
    public ResponseEntity<SuccessResponse<PostCommentResponse>> updateComment(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @Valid @RequestBody PostCommentRequest request
    ) {
        PostCommentResponse response = postCommentService.updateComment(
                authMember.getUser().getId(), postId, commentId, request);
        SuccessResponse<PostCommentResponse> successResponse = SuccessResponse.of(
                response,
                PostSuccessCode.POST_COMMENT_UPDATE_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 게시물 댓글 삭제 API (본인 댓글만)
    @DeleteMapping("/{commentId}")
    public ResponseEntity<SuccessResponse<PostCommentResponse>> deleteComment(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long postId,
            @PathVariable Long commentId
    ) {
        PostCommentResponse response = postCommentService.deleteComment(
                authMember.getUser().getId(), postId, commentId);
        SuccessResponse<PostCommentResponse> successResponse = SuccessResponse.of(
                response,
                PostSuccessCode.POST_COMMENT_DELETE_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }
}
