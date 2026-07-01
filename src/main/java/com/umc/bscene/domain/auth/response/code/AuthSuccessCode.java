package com.umc.bscene.domain.auth.response.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.umc.bscene.global.constant.StaticValue.CREATED;
import static com.umc.bscene.global.constant.StaticValue.OK;

@Getter
@RequiredArgsConstructor
public enum AuthSuccessCode implements BaseResponseCode {

    SIGNUP_SUCCESS(CREATED, "AUTH201_1", "회원가입에 성공했습니다."),
    LOGIN_ID_CHECK_SUCCESS(OK, "AUTH200_1", "로그인 아이디 중복 확인에 성공했습니다."),
    LOGIN_SUCCESS(OK, "AUTH200_2", "로그인에 성공했습니다.");

    private final int status;
    private final String code;
    private final String message;
}