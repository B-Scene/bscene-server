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
    REPLAY_WATCH_SUCCESS("LIVE200_9", OK, "다시보기 재생에 성공했습니다."),
    LIVE_SUMMARY_SUCCESS("LIVE200_10", OK, "라이브 종료 화면 조회에 성공했습니다."),
    RESERVATION_EDIT_VIEW_SUCCESS("LIVE200_11", OK, "라이브 예약 편집 화면 조회에 성공했습니다."),
    RESERVATION_UPDATE_SUCCESS("LIVE200_12", OK, "라이브 예약 수정에 성공했습니다."),
    RESERVATION_CANCEL_SUCCESS("LIVE200_13", OK, "라이브 예약 취소에 성공했습니다."),
    LIVE_CREATE_SUCCESS("LIVE201_1", CREATED, "라이브 생성에 성공했습니다."),
    REPLAY_SAVE_REQUEST_SUCCESS("LIVE202_1", 202, "다시보기 저장 요청이 접수되었습니다."),
    ;

    private final String code;
    private final int status;
    private final String message;
}
