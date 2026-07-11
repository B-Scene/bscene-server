package com.umc.bscene.domain.stream.enums.code.error;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StreamErrorCode implements BaseResponseCode {

    NO_ACTIVE_BAND_PROFILE("LIVE400_1", 400, "활성화된 밴드 멤버 프로필이 없습니다. 방송에 사용할 밴드 프로필을 먼저 활성화해주세요."),
    ALARM_TARGET_NOT_SCHEDULED("LIVE400_2", 400, "예정된 라이브가 아니어서 알림을 설정할 수 없습니다."),
    FORBIDDEN_REQUEST("LIVE403_1", 403, "해당 리소스에 대한 접근 권한이 없습니다."),
    FAN_MODE_ONLY("LIVE403_2", 403, "팬 모드에서만 이용할 수 있는 기능입니다."),
    AUDIO_STREAM_NOT_FOUND("LIVE404_1", 404, "오디오 송출 세션을 찾을 수 없습니다."),
    AUDIO_STREAM_NOT_LIVE("LIVE404_2", 404, "현재 방송 중인 라이브가 아닙니다."),
    ALREADY_LIVE("LIVE409_1", 409, "이미 라이브를 진행중이므로, 새로운 라이브를 시작할 수 없습니다."),
    DB_CONSTRAINTS_FAILED("LIVE409_2", 409, "서버 내에서 라이브 경로 충돌이 발생했습니다."),
    TOO_MANY_SCHEDULED_LIVES("LIVE409_3", 409, "예약 라이브가 최대 허용 개수를 초과했습니다."),
    ;

    private final String code;
    private final int status;
    private final String message;
}
