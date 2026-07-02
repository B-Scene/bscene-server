package com.umc.bscene.domain.oauth.dto.response;

import com.umc.bscene.domain.auth.dto.response.TokenResponse;

/**
 * 소셜 로그인 성공 응답.
 * - 기존 유저: isNewUser=false, token에 access/refresh 담김
 * - 신규 유저: isNewUser=true, signupToken 발급(온보딩 후 /auth/oauth/signup 에서 교환)
 */
public record OauthLoginResponse(
        boolean isNewUser,
        String signupToken,
        TokenResponse token
) {
    public static OauthLoginResponse ofNew(String signupToken) {
        return new OauthLoginResponse(true, signupToken, null);
    }

    public static OauthLoginResponse ofExisting(TokenResponse token) {
        return new OauthLoginResponse(false, null, token);
    }
}
