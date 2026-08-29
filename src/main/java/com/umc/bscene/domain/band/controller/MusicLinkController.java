package com.umc.bscene.domain.band.controller;

import com.umc.bscene.domain.band.dto.request.MusicLinkSaveRequest;
import com.umc.bscene.domain.band.dto.response.MusicLinkResponse;
import com.umc.bscene.domain.band.response.code.BandSuccessCode;
import com.umc.bscene.domain.band.service.BandService;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/bands/{bandId}/music-links")
public class MusicLinkController {

    private final BandService bandService;

    // 음원 링크 조회 API
    @GetMapping
    public ResponseEntity<SuccessResponse<MusicLinkResponse>> getMusicLink(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long bandId
    ) {
        MusicLinkResponse response = bandService.getMusicLink(authMember.getUser().getId(), bandId);
        SuccessResponse<MusicLinkResponse> successResponse = SuccessResponse.of(
                response,
                BandSuccessCode.MUSIC_LINK_GET_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }

    // 음원 링크 저장 API (등록/수정 upsert, 밴드 멤버만 가능)
    @PutMapping
    public ResponseEntity<SuccessResponse<MusicLinkResponse>> saveMusicLink(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long bandId,
            @Valid @RequestBody MusicLinkSaveRequest request
    ) {
        MusicLinkResponse response = bandService.saveMusicLink(authMember.getUser().getId(), bandId, request);
        SuccessResponse<MusicLinkResponse> successResponse = SuccessResponse.of(
                response,
                BandSuccessCode.MUSIC_LINK_SAVE_SUCCESS
        );

        return ResponseEntity.status(successResponse.getStatus()).body(successResponse);
    }
}
