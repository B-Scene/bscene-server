package com.umc.bscene.domain.band.dto.response;

import com.umc.bscene.domain.band.enums.BandMemberStatus;

public record BandMemberSearchItem(
        Long userId,
        String nickname,
        BandMemberStatus bandMemberStatus,
        boolean inviteAvailable
) {
}
