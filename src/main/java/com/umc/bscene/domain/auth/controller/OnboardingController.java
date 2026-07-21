package com.umc.bscene.domain.auth.controller;

import com.umc.bscene.domain.auth.dto.onboarding.request.OnboardingSaveRequest;
import com.umc.bscene.domain.auth.dto.onboarding.response.FanNicknameCheckResponse;
import com.umc.bscene.domain.auth.dto.onboarding.response.GenreResponse;
import com.umc.bscene.domain.auth.dto.onboarding.response.OnboardingStatusResponse;
import com.umc.bscene.domain.auth.dto.onboarding.response.RegionResponse;
import com.umc.bscene.domain.auth.enums.code.OnboardingSuccessCode;
import com.umc.bscene.domain.auth.service.onboarding.OnboardingService;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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

    // 지역 목록 조회 API
    @GetMapping("/regions")
    public ResponseEntity<SuccessResponse<List<RegionResponse>>> getRegions() {
        List<RegionResponse> response = onboardingService.getRegions();
        SuccessResponse<List<RegionResponse>> successResponse = SuccessResponse.of(
                response,
                OnboardingSuccessCode.REGIONS_GET_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 온보딩 정보 저장 API
    @PatchMapping("/onboarding")
    public ResponseEntity<SuccessResponse<OnboardingStatusResponse>> saveOnboarding(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody OnboardingSaveRequest request
    ) {
        OnboardingStatusResponse response = onboardingService.saveOnboarding(authMember, request);
        SuccessResponse<OnboardingStatusResponse> successResponse = SuccessResponse.of(
                response,
                OnboardingSuccessCode.ONBOARDING_SAVE_SUCCESS
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