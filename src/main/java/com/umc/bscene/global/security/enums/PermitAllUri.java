package com.umc.bscene.global.security.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PermitAllUri {

    AUTH_SIGNUP("/auth/signup"),
    AUTH_LOGIN_ID_CHECK("/auth/login-id/check"),
    AUTH_LOGIN("/auth/login"),
    AUTH_LOGIN_ID_FIND("/auth/login-id/find"),
    AUTH_PASSWORD_RESET("/auth/password-reset"),
    AUTH_LOGOUT("/auth/logout"),
    AUTH_REISSUE("/auth/reissue"),

    PHONE_VERIFICATIONS("/phone-verifications/**"),

    OAUTH2_AUTHORIZATION("/oauth2/**"),
    OAUTH2_CALLBACK("/oauth/callback/**"),
    OAUTH_SIGNUP("/auth/oauth/signup"),
    OAUTH_EXCHANGE("/auth/oauth/exchange"),

    GENRES("/genres"),
    REGIONS("/regions"),

    // MediaMTX 관련 API 개방
    INTERNAL_MEDIAMTX("/internal/mediamtx/**"),;

    private final String uri;
}