package com.umc.bscene.domain.band.dto.response;

import com.umc.bscene.domain.band.entity.BandMember;
import com.umc.bscene.domain.band.enums.BandMemberStatus;

import java.time.LocalDateTime;

public record BandMemberResponse(
        Long id,
        Long bandId,
        Long userId,
        Long sessionProfileId,
        String sessionNickname,
        BandMemberStatus status,
        LocalDateTime createdAt
) {
    public static BandMemberResponse from(BandMember bandMember) {
        return new BandMemberResponse(
                bandMember.getId(),
                bandMember.getBand().getId(),
                bandMember.getUser().getId(),
                bandMember.getSessionProfile() != null ? bandMember.getSessionProfile().getSessionProfileId() : null,
                bandMember.getSessionProfile() != null ? bandMember.getSessionProfile().getNickname() : null,
                bandMember.getStatus(),
                bandMember.getCreatedAt()
        );
    }
}
