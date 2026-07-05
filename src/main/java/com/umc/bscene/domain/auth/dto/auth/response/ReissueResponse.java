package com.umc.bscene.domain.auth.dto.auth.response;

import lombok.Builder;

@Builder
public record ReissueResponse(
        String grantType,
        String accessToken,
        String refreshToken,
        Long accessTokenExpiresIn
) {
}