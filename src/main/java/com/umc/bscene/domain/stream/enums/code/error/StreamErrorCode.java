package com.umc.bscene.domain.stream.enums.code.error;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StreamErrorCode implements BaseResponseCode {

    FORBIDDEN_REQUEST("STREAM403_1", 403, "해당 리소스에 대한 접근 권한이 없습니다."),
    AUDIO_STREAM_NOT_FOUND("STREAM404_1", 404, "오디오 송출 세션을 찾을 수 없습니다."),
    DUPLICATE_LIVE_CREATE_TRY("STREAM409_1", 409, "오디오 송출 세션은 한 유저에 1개만 생성 가능합니다."),
    DB_CONSTRAINTS_FAILED("STREAM409_2", 409, "서버 내에서 라이브 경로 충돌이 발생했습니다.");

    private final String code;
    private final int status;
    private final String message;
}
