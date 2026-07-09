package com.umc.bscene.domain.stream.enums.code.success;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.umc.bscene.global.constant.StaticValue.OK;

@Getter
@RequiredArgsConstructor
public enum StreamSuccessCode implements BaseResponseCode {

    LIVE_ROOM_ENTER_SUCCESS("LIVE200_1", OK, "라이브 방 진입에 성공했습니다."),
    LIVE_ROOM_LEAVE_SUCCESS("LIVE200_2", OK, "라이브 방에서 나갔습니다."),;

    private final String code;
    private final int status;
    private final String message;
}
