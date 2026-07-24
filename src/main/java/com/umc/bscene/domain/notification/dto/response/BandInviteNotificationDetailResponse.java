package com.umc.bscene.domain.notification.dto.response;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.band.enums.BandMemberStatus;
import com.umc.bscene.domain.band.enums.BandMemberType;

public record BandInviteNotificationDetailResponse(
        Long bandMemberId,
        Long bandId,
        String bandName,
        String bandProfileImageUrl,
        Genre genre,
        Region region,
        Long acceptedMemberCount,
        BandMemberType memberType,
        BandMemberStatus status,
        boolean actionable
) {
}