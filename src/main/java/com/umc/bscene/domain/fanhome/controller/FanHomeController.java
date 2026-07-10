package com.umc.bscene.domain.fanhome.controller;

import com.umc.bscene.domain.fanhome.dto.response.FanHomeResponse;
import com.umc.bscene.domain.fanhome.response.code.FanHomeSuccessCode;
import com.umc.bscene.domain.fanhome.service.FanHomeService;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FanHomeController {

    private final FanHomeService fanHomeService;

    // 팬모드 홈 통합 조회 API
    @GetMapping("/home")
    public ResponseEntity<SuccessResponse<FanHomeResponse>> getFanHome(
            @AuthenticationPrincipal AuthMember authMember
    ) {
        FanHomeResponse response = fanHomeService.getFanHome(authMember.getUser().getId());
        SuccessResponse<FanHomeResponse> successResponse = SuccessResponse.of(
                response,
                FanHomeSuccessCode.FAN_HOME_GET_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }
}
