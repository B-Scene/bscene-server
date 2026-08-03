package com.umc.bscene.domain.band.dto.response;

import com.umc.bscene.domain.band.entity.BandMember;
import com.umc.bscene.domain.band.enums.BandMemberStatus;
import com.umc.bscene.domain.band.enums.BandMemberType;
import com.umc.bscene.domain.session.enums.Part;

import java.time.LocalDateTime;

public record BandMemberResponse(
        Long id,
        Long bandId,
        Long userId,
        Long bandMemberProfileId,
        String profileNickname,
        Part part,
        boolean owner,
        BandMemberType memberType,
        BandMemberStatus status,
        LocalDateTime createdAt
) {
    public static BandMemberResponse from(BandMember bandMember) {
        return new BandMemberResponse(
                bandMember.getId(),
                bandMember.getBand().getId(),
                bandMember.getUser().getId(),
                bandMember.getBandMemberProfile() != null
                        ? bandMember.getBandMemberProfile().getId()
                        : null,
                bandMember.getBandMemberProfile() != null
                        ? bandMember.getBandMemberProfile().getNickname()
                        : null,
                bandMember.getBandMemberProfile() != null
                        ? bandMember.getBandMemberProfile().getPart()
                        : null,
                bandMember.getBand().getOwner().getId().equals(bandMember.getUser().getId()),
                bandMember.getMemberType(),
                bandMember.getStatus(),
                bandMember.getCreatedAt()
        );
    }
}
