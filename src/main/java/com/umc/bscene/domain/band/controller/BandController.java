package com.umc.bscene.domain.band.controller;

import com.umc.bscene.domain.band.dto.request.BandCreateRequest;
import com.umc.bscene.domain.band.dto.response.BandNameCheckResponse;
import com.umc.bscene.domain.band.dto.response.BandProfileResponse;
import com.umc.bscene.domain.band.dto.response.BandResponse;
import com.umc.bscene.domain.band.response.code.BandSuccessCode;
import com.umc.bscene.domain.band.service.BandService;
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

    @GetMapping("/{bandId}")
    public ResponseEntity<SuccessResponse<BandProfileResponse>> getBandProfile(
            @PathVariable Long bandId
    ) {
        BandProfileResponse response = bandService.getBandProfile(bandId);
        SuccessResponse<BandProfileResponse> successResponse = SuccessResponse.of(
                response,
                BandSuccessCode.BAND_PROFILE_GET_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }
}
