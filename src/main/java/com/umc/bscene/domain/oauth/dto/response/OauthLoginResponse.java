package com.umc.bscene.domain.oauth.dto.response;

import com.umc.bscene.domain.auth.dto.response.TokenResponse;

/**
 * 소셜 로그인 성공 응답.
 * - 기존 유저: isNewUser=false, token에 access/refresh 담김
 * - 신규 유저: isNewUser=true, signupToken 발급(온보딩 후 /auth/oauth/signup 에서 교환)
 *   email은 소셜 제공자에서 받아온 아이디(이메일)로, 프론트가 온보딩 화면 아이디 칸을 채우는 용도
 */
public record OauthLoginResponse(
        boolean isNewUser,
        String signupToken,
        String email,
        TokenResponse token
) {
    public static OauthLoginResponse ofNew(String signupToken, String email) {
        return new OauthLoginResponse(true, signupToken, email, null);
    }

    public static OauthLoginResponse ofExisting(TokenResponse token) {
        return new OauthLoginResponse(false, null, null, token);
    }
}
