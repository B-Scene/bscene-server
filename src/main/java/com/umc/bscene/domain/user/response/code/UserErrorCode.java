package com.umc.bscene.domain.user.response.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import static com.umc.bscene.global.constant.StaticValue.*;

@Getter @RequiredArgsConstructor
public enum UserErrorCode implements BaseResponseCode {
    USER_NOT_BLOCKED(NOT_FOUND, "USER404_3", "차단하지 않은 사용자입니다."),
    FAN_PROFILE_NOT_FOUND(NOT_FOUND, "USER404_1", "팬 프로필을 찾을 수 없습니다."),
    USER_NOT_FOUND(NOT_FOUND, "USER404_2", "사용자를 찾을 수 없습니다."),
    SELF_BLOCK_NOT_ALLOWED(BAD_REQUEST, "USER400_1", "본인은 차단할 수 없습니다."),
    USER_ALREADY_BLOCKED(CONFLICT, "USER409_1", "이미 차단한 사람입니다."),
    DUPLICATE_NICKNAME(CONFLICT, "USER409_2", "이미 사용 중인 닉네임입니다.");

    private final int status;
    private final String code;
    private final String message;
}
