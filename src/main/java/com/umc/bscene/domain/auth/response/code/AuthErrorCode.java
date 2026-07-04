package com.umc.bscene.domain.auth.response.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.umc.bscene.global.constant.StaticValue.*;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements BaseResponseCode {

    INVALID_SIGNUP_REQUEST(BAD_REQUEST, "AUTH400_1", "요청 값이 올바르지 않습니다."),
    PASSWORD_CONFIRM_NOT_MATCH(BAD_REQUEST, "AUTH400_2", "비밀번호와 비밀번호 확인이 일치하지 않습니다."),
    LOGIN_FAILED(BAD_REQUEST, "AUTH400_3", "아이디 또는 비밀번호가 올바르지 않습니다."),

    INVALID_REFRESH_TOKEN(UNAUTHORIZED, "AUTH401_4", "유효하지 않은 Refresh Token입니다."),

    UNAVAILABLE_ACCOUNT(FORBIDDEN, "AUTH403_1", "이용할 수 없는 계정입니다."),

    MEMBER_NOT_FOUND(NOT_FOUND, "AUTH404_1", "일치하는 회원 정보를 찾을 수 없습니다."),

    DUPLICATE_LOGIN_ID(CONFLICT, "AUTH409_1", "이미 사용 중인 로그인 아이디입니다."),
    DUPLICATE_PHONE(CONFLICT, "AUTH409_2", "이미 가입된 휴대폰 번호입니다.");

    private final int status;
    private final String code;
    private final String message;
}