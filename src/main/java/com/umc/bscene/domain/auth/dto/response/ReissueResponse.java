package com.umc.bscene.domain.auth.dto.response;

import lombok.Builder;

@Builder
public record ReissueResponse(
        String grantType,
        String accessToken,
        String refreshToken,
        Long accessTokenExpiresIn
) {
}