package com.umc.bscene.domain.band.controller;

import com.umc.bscene.domain.band.dto.request.BandCreateRequest;
import com.umc.bscene.domain.band.dto.response.BandResponse;
import com.umc.bscene.domain.band.response.code.BandSuccessCode;
import com.umc.bscene.domain.band.service.BandService;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
