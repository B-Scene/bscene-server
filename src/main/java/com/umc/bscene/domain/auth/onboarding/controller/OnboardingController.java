package com.umc.bscene.domain.auth.onboarding.controller;

import com.umc.bscene.domain.auth.dto.onboarding.response.FanNicknameCheckResponse;
import com.umc.bscene.domain.auth.dto.onboarding.response.GenreResponse;
import com.umc.bscene.domain.auth.dto.onboarding.response.OnboardingStatusResponse;
import com.umc.bscene.domain.auth.enums.code.OnboardingSuccessCode;
import com.umc.bscene.domain.auth.service.onboarding.OnboardingService;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    // 장르 목록 조회 API
    @GetMapping("/genres")
    public ResponseEntity<SuccessResponse<List<GenreResponse>>> getGenres() {
        List<GenreResponse> response = onboardingService.getGenres();
        SuccessResponse<List<GenreResponse>> successResponse = SuccessResponse.of(
                response,
                OnboardingSuccessCode.GENRES_GET_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 내 온보딩 상태 조회 API
    @GetMapping("/users/me/onboarding/status")
    public ResponseEntity<SuccessResponse<OnboardingStatusResponse>> getMyOnboardingStatus(
            @AuthenticationPrincipal AuthMember authMember
    ) {
        OnboardingStatusResponse response = onboardingService.getMyOnboardingStatus(authMember);
        SuccessResponse<OnboardingStatusResponse> successResponse = SuccessResponse.of(
                response,
                OnboardingSuccessCode.ONBOARDING_STATUS_GET_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 팬 닉네임 중복 확인 API
    @GetMapping("/onboarding/fan-nickname/check")
    public ResponseEntity<SuccessResponse<FanNicknameCheckResponse>> checkFanNickname(
            @RequestParam String nickname
    ) {
        FanNicknameCheckResponse response = onboardingService.checkFanNickname(nickname);
        SuccessResponse<FanNicknameCheckResponse> successResponse = SuccessResponse.of(
                response,
                OnboardingSuccessCode.FAN_NICKNAME_CHECK_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }
}