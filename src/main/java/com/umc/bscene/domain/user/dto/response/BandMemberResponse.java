package com.umc.bscene.domain.user.dto.response;

import java.util.List;

public record BandMemberResponse(
        Long bandMemberProfileId,
        String profileImageUrl,
        String nickname,
        String bandName,
        List<String> parts,
        Integer follower,
        Integer applicant,
        Integer performance,
        Boolean isBandMember
) {
}
