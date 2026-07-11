package com.umc.bscene.domain.user.response.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.umc.bscene.global.constant.StaticValue.*;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements BaseResponseCode {

    FAN_PROFILE_NOT_FOUND(NOT_FOUND, "USER404_1", "팬 프로필을 찾을 수 없습니다.");

    private final int status;
    private final String code;
    private final String message;
}
