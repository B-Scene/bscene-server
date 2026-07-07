package com.umc.bscene.domain.stream.enums.code.error;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StreamErrorCode implements BaseResponseCode {

    DUPLICATE_LIVE_CREATE_TRY("STREAM409_1", 409, "오디오 송출 세션은 한 유저에 1개만 생성 가능합니다.");

    private final String code;
    private final int status;
    private final String message;
}
