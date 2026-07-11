package com.umc.bscene.domain.stream.enums.code.error;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StreamErrorCode implements BaseResponseCode {

    NO_ACTIVE_BAND_PROFILE("LIVE400_1", 400, "활성화된 밴드 멤버 프로필이 없습니다. 방송에 사용할 밴드 프로필을 먼저 활성화해주세요."),
    INVALID_CO_HOST("LIVE400_2", 400, "공동 진행자는 생성자가 속한 밴드의 밴드 멤버여야 합니다."),
    FORBIDDEN_REQUEST("LIVE403_1", 403, "해당 리소스에 대한 접근 권한이 없습니다."),
    AUDIO_STREAM_NOT_FOUND("LIVE404_1", 404, "오디오 송출 세션을 찾을 수 없습니다."),
    AUDIO_STREAM_NOT_LIVE("LIVE404_2", 404, "현재 방송 중인 라이브가 아닙니다."),
    ALREADY_LIVE("LIVE409_1", 409, "이미 라이브를 진행중이므로, 새로운 라이브를 시작할 수 없습니다."),
    DB_CONSTRAINTS_FAILED("LIVE409_2", 409, "서버 내에서 라이브 경로 충돌이 발생했습니다."),
    AUDIO_STREAM_NOT_SCHEDULED("LIVE409_3", 409, "예약 상태의 라이브가 아니므로 요청을 처리할 수 없습니다."),
    CO_HOST_CONFLICT("LIVE409_4", 409, "공동 진행자 변경 요청이 동시에 처리되어 충돌이 발생했습니다."),
    ;

    private final String code;
    private final int status;
    private final String message;
}
