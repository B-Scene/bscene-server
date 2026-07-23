package com.umc.bscene.domain.band.dto.response;

import com.umc.bscene.domain.band.entity.BandInviteLink;
import com.umc.bscene.domain.band.enums.BandMemberType;

import java.time.LocalDateTime;

public record BandInviteLinkResponse(
        Long bandId,
        BandMemberType memberType,
        String token,
        LocalDateTime expiresAt
) {

    public static BandInviteLinkResponse from(
            BandInviteLink inviteLink
    ) {
        return new BandInviteLinkResponse(
                inviteLink.getBand().getId(),
                inviteLink.getMemberType(),
                inviteLink.getToken(),
                inviteLink.getExpiresAt()
        );
    }
}