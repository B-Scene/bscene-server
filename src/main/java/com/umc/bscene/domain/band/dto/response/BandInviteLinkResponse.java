package com.umc.bscene.domain.band.dto.response;

import com.umc.bscene.domain.band.entity.BandInviteLink;

import java.time.LocalDateTime;

public record BandInviteLinkResponse(
        Long bandId,
        String token,
        LocalDateTime expiresAt
) {

    public static BandInviteLinkResponse from(BandInviteLink inviteLink) {
        return new BandInviteLinkResponse(
                inviteLink.getBand().getId(),
                inviteLink.getToken(),
                inviteLink.getExpiresAt()
        );
    }
}