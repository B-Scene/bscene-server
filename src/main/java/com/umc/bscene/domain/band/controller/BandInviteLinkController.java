package com.umc.bscene.domain.band.controller;

import com.umc.bscene.domain.band.dto.response.BandInviteLinkResponse;
import com.umc.bscene.domain.band.response.code.BandSuccessCode;
import com.umc.bscene.domain.band.service.BandInviteLinkService;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/bands/{bandId}/invite-links")
public class BandInviteLinkController {

    private final BandInviteLinkService bandInviteLinkService;

    @PostMapping
    public ResponseEntity<SuccessResponse<BandInviteLinkResponse>>
    issueInviteLink(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long bandId
    ) {
        BandInviteLinkResponse response =
                bandInviteLinkService.issueInviteLink(
                        authMember.getUser().getId(),
                        bandId
                );

        SuccessResponse<BandInviteLinkResponse> successResponse =
                SuccessResponse.of(
                        response,
                        BandSuccessCode.BAND_INVITE_LINK_CREATE_SUCCESS
                );

        return ResponseEntity
                .status(successResponse.getStatus())
                .body(successResponse);
    }
}