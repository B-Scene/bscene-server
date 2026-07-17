package com.umc.bscene.domain.band.dto.response;

import com.umc.bscene.domain.band.entity.BandMember;
import com.umc.bscene.domain.band.entity.BandMemberProfile;
import com.umc.bscene.domain.session.enums.Part;

public record BandPublicMemberProfileResponse(
        Long bandMemberId,
        String nickname,
        Part part,
        boolean owner
) {

    public static BandPublicMemberProfileResponse from(
            BandMember bandMember,
            boolean owner
    ) {
        BandMemberProfile profile = bandMember.getBandMemberProfile();

        return new BandPublicMemberProfileResponse(
                bandMember.getId(),
                profile.getNickname(),
                profile.getPart(),
                owner
        );
    }
}