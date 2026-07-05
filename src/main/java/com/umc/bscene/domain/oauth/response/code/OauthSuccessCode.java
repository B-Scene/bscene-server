package com.umc.bscene.domain.oauth.response.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.umc.bscene.global.constant.StaticValue.CREATED;
import static com.umc.bscene.global.constant.StaticValue.OK;

@Getter
@RequiredArgsConstructor
public enum OauthSuccessCode implements BaseResponseCode {

    OAUTH_LOGIN_SUCCESS(OK, "OAUTH200_1", "소셜 로그인에 성공했습니다."),
    OAUTH_SIGNUP_SUCCESS(CREATED, "OAUTH201_1", "소셜 회원가입에 성공했습니다.");

    private final int status;
    private final String code;
    private final String message;
}
