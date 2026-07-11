package com.umc.bscene.domain.band.dto.response;

import com.umc.bscene.domain.band.entity.BandMemberProfile;
import com.umc.bscene.domain.session.enums.Part;

import java.time.LocalDateTime;

public record BandMemberProfileResponse(
        Long id,
        String nickname,
        Part part,
        LocalDateTime createdAt
) {
    public static BandMemberProfileResponse from(BandMemberProfile profile) {
        return new BandMemberProfileResponse(
                profile.getId(),
                profile.getNickname(),
                profile.getPart(),
                profile.getCreatedAt()
        );
    }
}
