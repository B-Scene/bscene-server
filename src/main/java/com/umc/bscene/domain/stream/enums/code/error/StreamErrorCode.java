package com.umc.bscene.domain.stream.enums.code.error;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StreamErrorCode implements BaseResponseCode {

    FORBIDDEN_REQUEST("LIVE403_1", 403, "해당 리소스에 대한 접근 권한이 없습니다."),
    AUDIO_STREAM_NOT_FOUND("LIVE404_1", 404, "오디오 송출 세션을 찾을 수 없습니다."),
    AUDIO_STREAM_NOT_LIVE("LIVE404_2", 404, "현재 방송 중인 라이브가 아닙니다."),
    REPLAY_NOT_FOUND("LIVE404_3", 404, "다시보기를 찾을 수 없습니다."),
    RECORDING_NOT_FOUND("LIVE404_4", 404, "저장된 녹화 파일이 없습니다."),
    ALREADY_LIVE("LIVE409_1", 409, "이미 라이브를 진행중이므로, 새로운 라이브를 시작할 수 없습니다."),
    DB_CONSTRAINTS_FAILED("LIVE409_2", 409, "서버 내에서 라이브 경로 충돌이 발생했습니다."),
    STREAM_NOT_CLOSED("LIVE409_3", 409, "아직 방송이 종료되지 않았습니다."),
    ;

    private final String code;
    private final int status;
    private final String message;
}
