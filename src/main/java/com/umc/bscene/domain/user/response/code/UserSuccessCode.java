package com.umc.bscene.domain.user.response.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.umc.bscene.global.constant.StaticValue.OK;

@Getter
@RequiredArgsConstructor
public enum UserSuccessCode implements BaseResponseCode {

    FAN_MYPAGE_GET_SUCCESS(OK, "USER200_1", "팬 모드 마이페이지에 성공했습니다.");

    private final int status;
    private final String code;
    private final String message;
}
