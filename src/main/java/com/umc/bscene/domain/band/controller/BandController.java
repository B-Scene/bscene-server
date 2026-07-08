package com.umc.bscene.domain.band.controller;

import com.umc.bscene.domain.band.dto.request.BandCreateRequest;
import com.umc.bscene.domain.band.dto.response.BandRecommendResponse;
import com.umc.bscene.domain.band.dto.response.BandResponse;
import com.umc.bscene.domain.band.response.code.BandSuccessCode;
import com.umc.bscene.domain.band.service.BandRecommendationService;
import com.umc.bscene.domain.band.service.BandService;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/bands")
public class BandController {

    private final BandService bandService;
    private final BandRecommendationService bandRecommendationService;

    // 밴드 개설 API
    @PostMapping
    public ResponseEntity<SuccessResponse<BandResponse>> createBand(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody BandCreateRequest request
    ) {
        BandResponse response = bandService.createBand(authMember.getUser().getId(), request);
        SuccessResponse<BandResponse> successResponse = SuccessResponse.of(
                response,
                BandSuccessCode.BAND_CREATE_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 밴드 추천 목록 조회 API (취향/활동 기반)
    @GetMapping("/recommendations")
    public ResponseEntity<SuccessResponse<BandRecommendResponse>> getRecommendedBands(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer size
    ) {
        BandRecommendResponse response = bandRecommendationService.getRecommendedBands(authMember.getUser().getId(), cursor, size);
        SuccessResponse<BandRecommendResponse> successResponse = SuccessResponse.of(
                response,
                BandSuccessCode.BAND_RECOMMEND_LIST_GET_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }
}
