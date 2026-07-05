package com.umc.bscene.domain.auth.dto.auth.response;

import lombok.Builder;

@Builder
public record AccessTokenResponse(
        String grantType,
        String accessToken,
        Long accessTokenExpiresIn
) {
}