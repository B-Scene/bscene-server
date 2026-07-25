package com.umc.bscene.domain.stream.dto;

import com.umc.bscene.domain.session.enums.Part;

public record CoHostCandidateInfo(
        Long userId,
        Long bandMemberId,
        Long bandMemberProfileId,
        String profileImageUrl,
        String nickname,
        String part
) {
    public CoHostCandidateInfo(
            Long userId,
            Long bandMemberId,
            Long bandMemberProfileId,
            String profileImageUrl,
            String nickname,
            Part part
    ) {
        this(userId, bandMemberId, bandMemberProfileId, profileImageUrl, nickname, part.getDescription());
    }
}
