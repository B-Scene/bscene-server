package com.umc.bscene.domain.user.controller;

import com.umc.bscene.domain.user.dto.response.FanMyPageResponse;
import com.umc.bscene.domain.user.response.code.UserSuccessCode;
import com.umc.bscene.domain.user.service.MyPageService;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;

    // 마이페이지 조회 API (팬/밴드 모드 공용 엔드포인트)
    @GetMapping("/users/me")
    public ResponseEntity<SuccessResponse<FanMyPageResponse>> getFanMyPage(
            @AuthenticationPrincipal AuthMember authMember
    ) {
        FanMyPageResponse response = myPageService.getFanMyPage(authMember.getUser());
        SuccessResponse<FanMyPageResponse> successResponse = SuccessResponse.of(
                response,
                UserSuccessCode.FAN_MYPAGE_GET_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }
}
