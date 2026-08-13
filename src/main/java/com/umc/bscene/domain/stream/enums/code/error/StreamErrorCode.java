package com.umc.bscene.domain.stream.enums.code.error;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StreamErrorCode implements BaseResponseCode {

    NO_ACTIVE_BAND_PROFILE("LIVE400_1", 400, "활성화된 밴드 멤버 프로필이 없습니다. 방송에 사용할 밴드 프로필을 먼저 활성화해주세요."),
    ALARM_TARGET_NOT_SCHEDULED("LIVE400_2", 400, "예정된 라이브가 아니어서 알림을 설정할 수 없습니다."),
    INVALID_CO_HOST("LIVE400_3", 400, "공동 진행자는 생성자가 속한 밴드의 밴드 멤버여야 합니다."),
    SELF_REPORT_NOT_ALLOWED("LIVE400_4", 400, "자기 자신은 신고할 수 없습니다."),
    FORBIDDEN_REQUEST("LIVE403_1", 403, "해당 리소스에 대한 접근 권한이 없습니다."),
    FAN_MODE_ONLY("LIVE403_2", 403, "팬 모드에서만 이용할 수 있는 기능입니다."),
    AUDIO_STREAM_NOT_FOUND("LIVE404_1", 404, "오디오 송출 세션을 찾을 수 없습니다."),
    AUDIO_STREAM_NOT_LIVE("LIVE404_2", 404, "현재 방송 중인 라이브가 아닙니다."),
    REPLAY_NOT_FOUND("LIVE404_3", 404, "다시보기를 찾을 수 없습니다."),
    RECORDING_NOT_FOUND("LIVE404_4", 404, "저장된 녹화 파일이 없습니다."),
    REPORT_TARGET_NOT_FOUND("LIVE404_5", 404, "신고 대상 유저를 찾을 수 없습니다."),
    CO_HOST_INVITATION_NOT_FOUND("LIVE404_6", 404, "공동 진행자 초대를 찾을 수 없습니다."),
    CO_HOST_UPGRADE_REQUEST_NOT_FOUND("LIVE404_7", 404, "공동 송출자 업그레이드 요청을 찾을 수 없습니다."),
    ALREADY_LIVE("LIVE409_1", 409, "이미 라이브를 진행중이므로, 새로운 라이브를 시작할 수 없습니다."),
    DB_CONSTRAINTS_FAILED("LIVE409_2", 409, "서버 내에서 라이브 경로 충돌이 발생했습니다."),
    TOO_MANY_SCHEDULED_LIVES("LIVE409_3", 409, "예약 라이브가 최대 허용 개수를 초과했습니다."),
    STREAM_NOT_CLOSED("LIVE409_4", 409, "아직 방송이 종료되지 않았습니다."),
    AUDIO_STREAM_NOT_SCHEDULED("LIVE409_5", 409, "예약 상태의 라이브가 아니므로 요청을 처리할 수 없습니다."),
    CO_HOST_CONFLICT("LIVE409_6", 409, "공동 진행자 변경 요청이 동시에 처리되어 충돌이 발생했습니다."),
    CO_HOST_INVITATION_ALREADY_PROCESSED("LIVE409_7", 409, "이미 처리된 공동 진행자 초대입니다."),
    CO_HOST_ALREADY_ACCEPTED("LIVE409_8", 409, "이미 공동 진행자로 확정된 멤버입니다."),
    CO_HOST_UPGRADE_ALREADY_REQUESTED("LIVE409_9", 409, "이미 처리 대기 중인 공동 송출자 업그레이드 요청이 있습니다."),
    ALREADY_REPORTED("LIVE409_10", 409, "이 라이브에서 이미 신고한 유저입니다."),
    ;

    private final String code;
    private final int status;
    private final String message;
}
