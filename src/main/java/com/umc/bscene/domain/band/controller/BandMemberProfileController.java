package com.umc.bscene.domain.band.controller;

import com.umc.bscene.domain.band.dto.request.BandMemberProfileCreateRequest;
import com.umc.bscene.domain.band.dto.request.BandMemberProfileUpdateRequest;
import com.umc.bscene.domain.band.dto.response.BandMemberProfileResponse;
import com.umc.bscene.domain.band.response.code.BandSuccessCode;
import com.umc.bscene.domain.band.service.BandMemberProfileService;
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
@RequestMapping("/band-member-profiles")
public class BandMemberProfileController {

    private final BandMemberProfileService bandMemberProfileService;

    // 멤버 프로필 생성 API
    @PostMapping
    public ResponseEntity<SuccessResponse<BandMemberProfileResponse>> createProfile(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody BandMemberProfileCreateRequest request
    ) {
        BandMemberProfileResponse response = bandMemberProfileService.createProfile(
                authMember.getUser().getId(), request
        );
        SuccessResponse<BandMemberProfileResponse> successResponse = SuccessResponse.of(
                response,
                BandSuccessCode.BAND_MEMBER_PROFILE_CREATE_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 내 멤버 프로필 목록 조회 API
    @GetMapping
    public ResponseEntity<SuccessResponse<List<BandMemberProfileResponse>>> getProfiles(
            @AuthenticationPrincipal AuthMember authMember
    ) {
        List<BandMemberProfileResponse> response = bandMemberProfileService.getProfiles(
                authMember.getUser().getId()
        );
        SuccessResponse<List<BandMemberProfileResponse>> successResponse = SuccessResponse.of(
                response,
                BandSuccessCode.BAND_MEMBER_PROFILE_LIST_GET_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 내 멤버 프로필 단건 조회 API
    @GetMapping("/{profileId}")
    public ResponseEntity<SuccessResponse<BandMemberProfileResponse>> getProfile(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long profileId
    ) {
        BandMemberProfileResponse response = bandMemberProfileService.getProfile(
                authMember.getUser().getId(), profileId
        );
        SuccessResponse<BandMemberProfileResponse> successResponse = SuccessResponse.of(
                response,
                BandSuccessCode.BAND_MEMBER_PROFILE_GET_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 멤버 프로필 수정 API
    @PatchMapping("/{profileId}")
    public ResponseEntity<SuccessResponse<BandMemberProfileResponse>> updateProfile(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long profileId,
            @Valid @RequestBody BandMemberProfileUpdateRequest request
    ) {
        BandMemberProfileResponse response = bandMemberProfileService.updateProfile(
                authMember.getUser().getId(), profileId, request
        );
        SuccessResponse<BandMemberProfileResponse> successResponse = SuccessResponse.of(
                response,
                BandSuccessCode.BAND_MEMBER_PROFILE_UPDATE_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 멤버 프로필 삭제 API
    @DeleteMapping("/{profileId}")
    public ResponseEntity<SuccessResponse<Void>> deleteProfile(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long profileId
    ) {
        bandMemberProfileService.deleteProfile(authMember.getUser().getId(), profileId);
        SuccessResponse<Void> successResponse = SuccessResponse.of(
                (Void) null,
                BandSuccessCode.BAND_MEMBER_PROFILE_DELETE_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }
}
