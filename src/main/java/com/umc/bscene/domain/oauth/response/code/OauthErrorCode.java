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
    INVALID_SIGNUP_INFO(BAD_REQUEST, "OAUTH400_3", "생년월일 또는 성별 정보가 올바르지 않습니다."),
    EMAIL_NOT_PROVIDED(BAD_REQUEST, "OAUTH400_4", "소셜 계정에서 이메일을 제공받지 못했습니다. 이메일 제공에 동의해 주세요."),
    INVALID_EXCHANGE_CODE(BAD_REQUEST, "OAUTH400_5", "유효하지 않거나 만료된 로그인 코드입니다."),
    ALREADY_REGISTERED(CONFLICT, "OAUTH409_1", "이미 가입된 소셜 계정입니다."),
    DUPLICATE_PHONE(CONFLICT, "OAUTH409_2", "이미 가입된 휴대폰 번호입니다.");

    private final int status;
    private final String code;
    private final String message;
}
