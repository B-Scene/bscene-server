package com.umc.bscene.domain.band.controller;

import com.umc.bscene.domain.band.dto.request.BandCreateRequest;
import com.umc.bscene.domain.band.dto.request.BandUpdateRequest;
import com.umc.bscene.domain.band.dto.response.BandDetailResponse;
import com.umc.bscene.domain.band.dto.response.BandNameCheckResponse;
import com.umc.bscene.domain.band.dto.response.BandProfileResponse;
import com.umc.bscene.domain.band.dto.response.BandRecommendResponse;
import com.umc.bscene.domain.band.dto.response.BandResponse;
import com.umc.bscene.domain.band.response.code.BandSuccessCode;
import com.umc.bscene.domain.band.service.BandRecommendationService;
import com.umc.bscene.domain.band.service.BandService;
import com.umc.bscene.domain.recommendation.service.InteractionService;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/bands")
public class BandController {

    private final BandService bandService;
    private final BandRecommendationService bandRecommendationService;
    private final InteractionService interactionService;

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

    // 밴드 추천 목록 조회 API
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

    // 밴드명 중복 체크 API
    @GetMapping("/check-name")
    public ResponseEntity<SuccessResponse<BandNameCheckResponse>> checkBandName(
            @RequestParam String name
    ) {
        BandNameCheckResponse response = bandService.checkBandName(name);
        BandSuccessCode successCode = response.available()
                ? BandSuccessCode.BAND_NAME_AVAILABLE
                : BandSuccessCode.BAND_NAME_DUPLICATED;
        SuccessResponse<BandNameCheckResponse> successResponse = SuccessResponse.of(
                response,
                successCode
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 밴드 프로필 조회
    @GetMapping("/{bandId}")
    public ResponseEntity<SuccessResponse<BandProfileResponse>> getBandProfile(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long bandId
    ) {
        BandProfileResponse response = bandService.getBandProfile(bandId);
        interactionService.recordClick(authMember.getUser().getId(), bandId);
        SuccessResponse<BandProfileResponse> successResponse = SuccessResponse.of(
                response,
                BandSuccessCode.BAND_PROFILE_GET_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 팬모드 밴드 상세 조회 API (밴드 프로필 화면 : 기본 정보 + 팔로우 여부 + 라이브 진행 여부)
    @GetMapping("/{bandId}/detail")
    public ResponseEntity<SuccessResponse<BandDetailResponse>> getBandDetail(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long bandId
    ) {
        BandDetailResponse response = bandService.getBandDetail(authMember.getUser().getId(), bandId);
        interactionService.recordClick(authMember.getUser().getId(), bandId);
        SuccessResponse<BandDetailResponse> successResponse = SuccessResponse.of(
                response,
                BandSuccessCode.BAND_DETAIL_GET_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 밴드 프로필 수정
    @PatchMapping("/{bandId}")
    public ResponseEntity<SuccessResponse<BandProfileResponse>> updateBandProfile(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long bandId,
            @RequestBody BandUpdateRequest request
    ) {
        BandProfileResponse response = bandService.updateBandProfile(
                authMember.getUser().getId(),
                bandId,
                request
        );
        SuccessResponse<BandProfileResponse> successResponse = SuccessResponse.of(
                response,
                BandSuccessCode.BAND_PROFILE_UPDATE_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }
}
