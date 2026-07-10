package com.umc.bscene.domain.band.dto.response;

import com.umc.bscene.domain.band.entity.BandMember;
import com.umc.bscene.domain.band.enums.BandMemberStatus;

public record BandMemberAcceptResponse(
        Long bandId,
        Long bandMemberProfileId,
        String nickname,
        BandMemberStatus status
) {
    public static BandMemberAcceptResponse from(BandMember bandMember) {
        return new BandMemberAcceptResponse(
                bandMember.getBand().getId(),
                bandMember.getBandMemberProfile().getId(),
                bandMember.getBandMemberProfile().getNickname(),
                bandMember.getStatus()
        );
    }
}
