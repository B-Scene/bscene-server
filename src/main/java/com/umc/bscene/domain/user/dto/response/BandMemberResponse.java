package com.umc.bscene.domain.user.dto.response;

import java.util.List;

public record BandMemberResponse(
        String profileImageUrl,
        String nickname,
        String bandName,
        List<String> parts
) {
}
