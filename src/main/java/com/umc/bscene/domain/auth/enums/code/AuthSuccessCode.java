package com.umc.bscene.domain.auth.enums.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.umc.bscene.global.constant.StaticValue.OK;

@Getter
@RequiredArgsConstructor
public enum AuthSuccessCode implements BaseResponseCode {

    LOGIN_SUCCESS(OK, "AUTH200_2", "로그인에 성공했습니다."),
    LOGOUT_SUCCESS(OK, "AUTH200_5", "로그아웃되었습니다."),
    TOKEN_REISSUE_SUCCESS(OK, "AUTH200_6", "토큰이 재발급되었습니다.");

    private final int status;
    private final String code;
    private final String message;
}
