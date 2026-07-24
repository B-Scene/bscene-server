package com.umc.bscene.domain.band.dto.response;

import com.umc.bscene.domain.band.entity.BandMember;
import com.umc.bscene.domain.band.enums.BandMemberStatus;
import com.umc.bscene.domain.band.enums.BandMemberType;

public record BandInviteLinkEntryResponse(
        Long bandMemberId,
        Long bandId,
        BandMemberType memberType,
        BandMemberStatus status
) {

    public static BandInviteLinkEntryResponse from(
            BandMember bandMember
    ) {
        return new BandInviteLinkEntryResponse(
                bandMember.getId(),
                bandMember.getBand().getId(),
                bandMember.getMemberType(),
                bandMember.getStatus()
        );
    }
}