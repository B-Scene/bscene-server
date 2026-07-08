package com.umc.bscene.domain.band.dto.response;

import com.umc.bscene.domain.band.entity.BandMember;
import com.umc.bscene.domain.band.enums.BandMemberStatus;

public record BandMemberAcceptResponse(
        Long bandId,
        Long bandProfileId,
        String nickname,
        BandMemberStatus status
) {
    public static BandMemberAcceptResponse from(BandMember bandMember) {
        return new BandMemberAcceptResponse(
                bandMember.getBand().getId(),
                bandMember.getBandProfile().getBandProfileId(),
                bandMember.getBandProfile().getNickname(),
                bandMember.getStatus()
        );
    }
}