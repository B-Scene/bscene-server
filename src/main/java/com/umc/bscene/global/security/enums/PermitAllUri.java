package com.umc.bscene.global.security.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PermitAllUri {

    AUTH_LOGIN("/auth/login"),
    AUTH_LOGOUT("/auth/logout"),
    AUTH_REISSUE("/auth/reissue"),

    // 밴드 초대 링크 조회
    BAND_INVITE_LINK_LOOKUP("/band-invite-links/*"),

    OAUTH2_AUTHORIZATION("/oauth2/**"),
    OAUTH2_CALLBACK("/oauth/callback/**"),
    OAUTH_SIGNUP("/auth/oauth/signup"),
    OAUTH_EXCHANGE("/auth/oauth/exchange"),

    // 모바일 SDK 소셜 로그인 (앱이 받은 제공자 토큰을 서버가 검증). 로그인 전 호출이라 허용
    OAUTH_MOBILE_KAKAO("/auth/oauth/mobile/kakao"),
    OAUTH_MOBILE_APPLE("/auth/oauth/mobile/apple"),
    OAUTH_MOBILE_APPLE_CHALLENGE("/auth/oauth/mobile/apple/challenge"),

    GENRES("/genres"),
    REGIONS("/regions"),
    CHAT_WEBSOCKET("/ws/chat"),
    LIVE_CHAT_WEBSOCKET("/ws/lives/*/chat"),

    // MediaMTX 관련 API 개방
    INTERNAL_MEDIAMTX("/internal/mediamtx/**"),

    // 검색 색인 관리용 내부 API
    INTERNAL_SEARCH("/internal/search/**"),

    SPRING_ACTUATOR("/actuator/**"),
    ;

    private final String uri;
}
