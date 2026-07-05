package com.umc.bscene.domain.auth.enums.code;

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
    LOGIN_SUCCESS(OK, "AUTH200_2", "로그인에 성공했습니다."),
    LOGIN_ID_FIND_SUCCESS(OK, "AUTH200_3", "아이디 조회에 성공했습니다."),
    PASSWORD_RESET_SUCCESS(OK, "AUTH200_4", "비밀번호가 변경되었습니다."),
    LOGOUT_SUCCESS(OK, "AUTH200_5", "로그아웃되었습니다."),
    TOKEN_REISSUE_SUCCESS(OK, "AUTH200_6", "토큰이 재발급되었습니다.");

    private final int status;
    private final String code;
    private final String message;
}