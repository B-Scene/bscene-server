package com.umc.bscene.global.security.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PermitAllUri {

    AUTH_SIGNUP("/auth/signup"),
    AUTH_LOGIN_ID_CHECK("/auth/login-id/check"),
    AUTH_LOGIN("/auth/login"),

    PHONE_VERIFICATIONS("/phone-verifications/**"),

    OAUTH2_AUTHORIZATION("/oauth2/**"),
    OAUTH2_CALLBACK("/login/oauth2/**");

    private final String uri;
}