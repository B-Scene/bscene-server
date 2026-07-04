package com.umc.bscene.domain.onboarding.controller;

import com.umc.bscene.domain.onboarding.dto.response.GenreResponse;
import com.umc.bscene.domain.onboarding.response.code.OnboardingSuccessCode;
import com.umc.bscene.domain.onboarding.service.OnboardingService;
import com.umc.bscene.global.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
}