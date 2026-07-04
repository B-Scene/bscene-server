package com.umc.bscene.domain.auth.dto.response;

import lombok.Builder;

@Builder
public record AccessTokenResponse(
        String grantType,
        String accessToken,
        Long accessTokenExpiresIn
) {
}