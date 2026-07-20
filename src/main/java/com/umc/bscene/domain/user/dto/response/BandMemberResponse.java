package com.umc.bscene.domain.user.dto.response;

import java.util.List;

public record BandMemberResponse(
        Long bandId,
        String profileImageUrl,
        String nickname,
        String bandName,
        List<String> parts
) {
}
