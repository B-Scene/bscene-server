package com.umc.bscene.domain.post.controller;

import com.umc.bscene.domain.post.dto.request.PostCreateRequest;
import com.umc.bscene.domain.post.dto.request.PostUpdateRequest;
import com.umc.bscene.domain.post.dto.response.PostCreateResponse;
import com.umc.bscene.domain.post.dto.response.PostListResponse;
import com.umc.bscene.domain.post.dto.response.PostResponse;
import com.umc.bscene.domain.post.dto.response.PostUpdateResponse;
import com.umc.bscene.domain.post.enums.PostType;
import com.umc.bscene.domain.post.response.code.PostSuccessCode;
import com.umc.bscene.domain.post.service.PostService;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // 콘텐츠 등록 API (밴드 멤버만 가능)
    @PostMapping("/bands/{bandId}/posts")
    public ResponseEntity<SuccessResponse<PostCreateResponse>> createPost(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long bandId,
            @Valid @RequestBody PostCreateRequest request
    ) {
        PostCreateResponse response = postService.createPost(authMember.getUser().getId(), bandId, request);
        SuccessResponse<PostCreateResponse> successResponse = SuccessResponse.of(
                response,
                PostSuccessCode.POST_CREATE_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 콘텐츠 목록 조회 API (type 필터링, 커서 페이지네이션)
    @GetMapping("/bands/{bandId}/posts")
    public ResponseEntity<SuccessResponse<PostListResponse>> getPosts(
            @PathVariable Long bandId,
            @RequestParam(required = false) PostType type,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer size
    ) {
        PostListResponse response = postService.getPosts(bandId, type, cursor, size);
        SuccessResponse<PostListResponse> successResponse = SuccessResponse.of(
                response,
                PostSuccessCode.POST_LIST_GET_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 콘텐츠 상세 조회 API (수정 화면 prefill 겸용)
    @GetMapping("/posts/{postId}")
    public ResponseEntity<SuccessResponse<PostResponse>> getPostDetail(
            @PathVariable Long postId
    ) {
        PostResponse response = postService.getPostDetail(postId);
        SuccessResponse<PostResponse> successResponse = SuccessResponse.of(
                response,
                PostSuccessCode.POST_DETAIL_GET_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 콘텐츠 수정 API (밴드 멤버만 가능, type 수정 불가)
    @PutMapping("/posts/{postId}")
    public ResponseEntity<SuccessResponse<PostUpdateResponse>> updatePost(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long postId,
            @RequestBody PostUpdateRequest request
    ) {
        PostUpdateResponse response = postService.updatePost(authMember.getUser().getId(), postId, request);
        SuccessResponse<PostUpdateResponse> successResponse = SuccessResponse.of(
                response,
                PostSuccessCode.POST_UPDATE_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 콘텐츠 삭제 API (밴드 멤버만 가능)
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<SuccessResponse<Void>> deletePost(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long postId
    ) {
        postService.deletePost(authMember.getUser().getId(), postId);
        SuccessResponse<Void> successResponse = SuccessResponse.of(
                (Void) null,
                PostSuccessCode.POST_DELETE_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }
}
