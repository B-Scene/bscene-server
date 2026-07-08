package com.umc.bscene.domain.band.dto.response;

import com.umc.bscene.domain.band.entity.BandMember;
import com.umc.bscene.domain.band.enums.BandMemberStatus;

public record BandMemberAcceptResponse(
        Long bandId,
        Long sessionProfileId,
        String nickname,
        BandMemberStatus status
) {
    public static BandMemberAcceptResponse from(BandMember bandMember) {
        return new BandMemberAcceptResponse(
                bandMember.getBand().getId(),
                bandMember.getSessionProfile().getSessionProfileId(),
                bandMember.getSessionProfile().getNickname(),
                bandMember.getStatus()
        );
    }
}
