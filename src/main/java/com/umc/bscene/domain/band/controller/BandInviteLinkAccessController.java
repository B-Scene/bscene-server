package com.umc.bscene.domain.band.controller;

import com.umc.bscene.domain.band.dto.response.BandInviteLinkDetailResponse;
import com.umc.bscene.domain.band.response.code.BandSuccessCode;
import com.umc.bscene.domain.band.service.BandInviteLinkService;
import com.umc.bscene.global.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/band-invite-links")
public class BandInviteLinkAccessController {

    private final BandInviteLinkService bandInviteLinkService;

    @GetMapping("/{token}")
    public ResponseEntity<
            SuccessResponse<BandInviteLinkDetailResponse>
            > getInviteLink(
            @PathVariable String token
    ) {
        BandInviteLinkDetailResponse response =
                bandInviteLinkService.getInviteLink(token);

        SuccessResponse<BandInviteLinkDetailResponse> successResponse =
                SuccessResponse.of(
                        response,
                        BandSuccessCode.BAND_INVITE_LINK_GET_SUCCESS
                );

        return ResponseEntity
                .status(successResponse.getStatus())
                .body(successResponse);
    }
}