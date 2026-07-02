package com.umc.bscene.domain.oauth.response.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.umc.bscene.global.constant.StaticValue.BAD_REQUEST;
import static com.umc.bscene.global.constant.StaticValue.CONFLICT;

@Getter
@RequiredArgsConstructor
public enum OauthErrorCode implements BaseResponseCode {

    INVALID_SIGNUP_TOKEN(BAD_REQUEST, "OAUTH400_1", "유효하지 않은 회원가입 토큰입니다."),
    NOT_SUPPORT_PROVIDER(BAD_REQUEST, "OAUTH400_2", "지원하지 않는 소셜 로그인입니다."),
    ALREADY_REGISTERED(CONFLICT, "OAUTH409_1", "이미 가입된 소셜 계정입니다.");

    private final int status;
    private final String code;
    private final String message;
}
