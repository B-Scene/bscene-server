package com.umc.bscene.domain.band.dto.response;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.entity.BandInviteLink;
import com.umc.bscene.domain.band.enums.BandMemberType;

import java.time.LocalDateTime;

public record BandInviteLinkDetailResponse(
        Long bandId,
        String bandName,
        String bandProfileImageUrl,
        Genre genre,
        Region region,
        BandMemberType memberType,
        LocalDateTime expiresAt
) {

    public static BandInviteLinkDetailResponse from(
            BandInviteLink inviteLink
    ) {
        Band band = inviteLink.getBand();

        return new BandInviteLinkDetailResponse(
                band.getId(),
                band.getName(),
                band.getProfileImageUrl(),
                band.getGenre(),
                band.getRegion(),
                inviteLink.getMemberType(),
                inviteLink.getExpiresAt()
        );
    }
}