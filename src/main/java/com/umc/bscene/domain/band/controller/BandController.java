package com.umc.bscene.domain.band.controller;

import com.umc.bscene.domain.band.dto.FollowerBlock;
import com.umc.bscene.domain.band.dto.request.BandCreateRequest;
import com.umc.bscene.domain.band.dto.request.BandOwnerTransferRequest;
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
import com.umc.bscene.global.response.CursorPage;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
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
    // withDummy=false면 더미 밴드(운영 DB 시드 id 1~474)를 추천 후보에서 제외, 생략·true면 전체 (기존 호환)
    @GetMapping("/recommendations")
    public ResponseEntity<SuccessResponse<BandRecommendResponse>> getRecommendedBands(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer size,
            @RequestParam(defaultValue = "true") boolean withDummy
    ) {
        BandRecommendResponse response = bandRecommendationService.getRecommendedBands(authMember.getUser().getId(), cursor, size, withDummy);
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
        BandProfileResponse response = bandService.getBandProfile(authMember.getUser().getId(), bandId);
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

    // 밴드 멤버 자기 탈퇴 API
    @DeleteMapping("/{bandId}/leave")
    public ResponseEntity<SuccessResponse<Void>> leaveBand(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long bandId
    ) {
        bandService.leaveBand(authMember.getUser().getId(), bandId);

        SuccessResponse<Void> successResponse = SuccessResponse.of(
                (Void) null,
                BandSuccessCode.BAND_MEMBER_LEAVE_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 밴드 Owner 양도
    @PatchMapping("/{bandId}/owner")
    public ResponseEntity<SuccessResponse<Void>> transferOwnership(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long bandId,
            @Valid @RequestBody BandOwnerTransferRequest request
    ) {
        bandService.transferOwnership(authMember.getUser().getId(), bandId, request);

        SuccessResponse<Void> successResponse = SuccessResponse.of(
                (Void) null,
                BandSuccessCode.BAND_OWNER_TRANSFER_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 밴드모드 팔로워 목록 조회 API (현재 활성화된 멤버 프로필이 소속된 밴드의 팔로워 커서 페이징)
    @GetMapping("/follower")
    public ResponseEntity<SuccessResponse<CursorPage<FollowerBlock>>> getMyBandFollowers(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") @Min(1) @Max(15) Integer size
    ) {
        CursorPage<FollowerBlock> response = bandService.findPagedMyFollowers(
                authMember.getUser().getId(),
                cursor,
                size
        );
        SuccessResponse<CursorPage<FollowerBlock>> successResponse = SuccessResponse.of(
                response,
                BandSuccessCode.BAND_FOLLOWER_LIST_GET_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }
}
