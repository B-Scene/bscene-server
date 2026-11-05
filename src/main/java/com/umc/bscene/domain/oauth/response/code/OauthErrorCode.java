package com.umc.bscene.domain.oauth.response.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.umc.bscene.global.constant.StaticValue.BAD_REQUEST;
import static com.umc.bscene.global.constant.StaticValue.CONFLICT;
import static com.umc.bscene.global.constant.StaticValue.SERVICE_UNAVAILABLE;
import static com.umc.bscene.global.constant.StaticValue.TOO_MANY_REQUESTS;
import static com.umc.bscene.global.constant.StaticValue.UNAUTHORIZED;

@Getter
@RequiredArgsConstructor
public enum OauthErrorCode implements BaseResponseCode {

    INVALID_SIGNUP_TOKEN(BAD_REQUEST, "OAUTH400_1", "유효하지 않은 회원가입 토큰입니다."),
    NOT_SUPPORT_PROVIDER(BAD_REQUEST, "OAUTH400_2", "지원하지 않는 소셜 로그인입니다."),
    INVALID_SIGNUP_INFO(BAD_REQUEST, "OAUTH400_3", "생년월일 또는 성별 정보가 올바르지 않습니다."),
    EMAIL_NOT_PROVIDED(BAD_REQUEST, "OAUTH400_4", "소셜 계정에서 이메일을 제공받지 못했습니다. 이메일 제공에 동의해 주세요."),
    INVALID_EXCHANGE_CODE(BAD_REQUEST, "OAUTH400_5", "유효하지 않거나 만료된 로그인 코드입니다."),
    // 모바일 SDK 로그인 (명세 SDK v0.3)
    INVALID_APPLE_CHALLENGE(BAD_REQUEST, "OAUTH400_6", "유효하지 않거나 만료된 Apple 로그인 요청입니다."),
    INVALID_PROVIDER_TOKEN(UNAUTHORIZED, "OAUTH401_1", "유효하지 않거나 만료된 소셜 인증 정보입니다."),
    PROVIDER_TOKEN_APP_MISMATCH(UNAUTHORIZED, "OAUTH401_2", "비씬에서 사용할 수 없는 소셜 인증 정보입니다."),
    APPLE_NONCE_MISMATCH(UNAUTHORIZED, "OAUTH401_3", "Apple 로그인 요청 정보가 일치하지 않습니다."),
    ALREADY_REGISTERED(CONFLICT, "OAUTH409_1", "이미 가입된 소셜 계정입니다."),
    DUPLICATE_PHONE(CONFLICT, "OAUTH409_2", "이미 가입된 휴대폰 번호입니다."),
    TOO_MANY_LOGIN_ATTEMPTS(TOO_MANY_REQUESTS, "OAUTH429_1", "소셜 로그인 요청이 너무 많습니다. 잠시 후 다시 시도해 주세요."),
    PROVIDER_UNAVAILABLE(SERVICE_UNAVAILABLE, "OAUTH503_1", "소셜 인증 서비스에 일시적으로 연결할 수 없습니다.");

    private final int status;
    private final String code;
    private final String message;
}
