package com.umc.bscene.domain.band.dto.response;

public record BandMemberSearchItem(
        Long userId,
        String nickname,
        boolean alreadyMember
) {
}
