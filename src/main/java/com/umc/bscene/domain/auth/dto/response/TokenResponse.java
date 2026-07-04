package com.umc.bscene.domain.auth.dto.response;

public record TokenResponse(
        String grantType,
        String accessToken,
        String refreshToken,
        Long accessTokenExpiresIn,
        LoginUserResponse user
) {
}