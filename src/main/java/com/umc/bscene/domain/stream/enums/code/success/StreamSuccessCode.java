package com.umc.bscene.domain.stream.enums.code.success;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.umc.bscene.global.constant.StaticValue.CREATED;
import static com.umc.bscene.global.constant.StaticValue.OK;

@Getter
@RequiredArgsConstructor
public enum StreamSuccessCode implements BaseResponseCode {

    LIVE_ROOM_ENTER_SUCCESS("LIVE200_1", OK, "라이브 방 진입에 성공했습니다."),
    LIVE_ROOM_LEAVE_SUCCESS("LIVE200_2", OK, "라이브 방에서 나갔습니다."),
    ALL_LIVE_SUCCESS("LIVE200_3", OK, "현재 라이브 중인 목록을 조회하는데 성공했습니다."),
    LIVE_CLOSE_SUCCESS("LIVE200_4", OK, "라이브 종료에 성공했습니다."),
    LIVE_HOME_SUCCESS("LIVE200_5", OK, "라이브 홈 조회에 성공했습니다."),
    FOLLOWING_LIVE_SUCCESS("LIVE200_6", OK, "팔로우한 밴드의 라이브 중인 목록을 조회하는데 성공했습니다."),
    SCHEDULED_LIVE_SUCCESS("LIVE200_7", OK, "예정된 라이브 목록을 조회하는데 성공했습니다."),
    LIVE_ALARM_TOGGLE_SUCCESS("LIVE200_8", OK, "라이브 알림 설정 변경에 성공했습니다."),
    LIVE_CREATE_SUCCESS("LIVE201_1", CREATED, "라이브 생성에 성공했습니다."),
    ;

    private final String code;
    private final int status;
    private final String message;
}
