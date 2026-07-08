package com.umc.bscene.domain.follow.controller;

import com.umc.bscene.domain.follow.dto.response.FollowResponse;
import com.umc.bscene.domain.follow.response.code.FollowSuccessCode;
import com.umc.bscene.domain.follow.service.FollowService;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/bands/{bandId}/follow")
public class FollowController {

    private final FollowService followService;

    // 밴드 팔로우 API
    @PostMapping
    public ResponseEntity<SuccessResponse<FollowResponse>> follow(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long bandId
    ) {
        FollowResponse response = followService.follow(authMember, bandId);
        SuccessResponse<FollowResponse> successResponse = SuccessResponse.of(
                response,
                FollowSuccessCode.FOLLOW_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 밴드 팔로우 취소 API
    @DeleteMapping
    public ResponseEntity<SuccessResponse<FollowResponse>> unfollow(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long bandId
    ) {
        FollowResponse response = followService.unfollow(authMember, bandId);
        SuccessResponse<FollowResponse> successResponse = SuccessResponse.of(
                response,
                FollowSuccessCode.UNFOLLOW_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }
}
