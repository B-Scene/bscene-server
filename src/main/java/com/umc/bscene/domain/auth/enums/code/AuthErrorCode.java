package com.umc.bscene.domain.auth.enums.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.umc.bscene.global.constant.StaticValue.*;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements BaseResponseCode {

    LOGIN_FAILED(BAD_REQUEST, "AUTH400_3", "아이디 또는 비밀번호가 올바르지 않습니다."),

    INVALID_REFRESH_TOKEN(UNAUTHORIZED, "AUTH401_4", "유효하지 않은 Refresh Token입니다."),

    SUSPENDED_ACCOUNT(FORBIDDEN, "AUTH403_1", "정지된 계정입니다."),
    INACTIVE_ACCOUNT(FORBIDDEN, "AUTH403_2", "휴면 계정입니다. 휴면 해제가 필요합니다."),
    DELETED_ACCOUNT(FORBIDDEN, "AUTH403_3", "탈퇴한 계정입니다."),

    MEMBER_NOT_FOUND(NOT_FOUND, "AUTH404_1", "일치하는 회원 정보를 찾을 수 없습니다.");

    private final int status;
    private final String code;
    private final String message;
}
