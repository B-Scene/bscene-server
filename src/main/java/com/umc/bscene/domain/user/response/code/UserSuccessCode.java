package com.umc.bscene.domain.user.response.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import static com.umc.bscene.global.constant.StaticValue.OK;

@Getter @RequiredArgsConstructor
public enum UserSuccessCode implements BaseResponseCode {
    USER_UNBLOCK_SUCCESS(OK, "USER200_5", "사용자 차단을 해제했습니다."),
    FAN_MYPAGE_GET_SUCCESS(OK, "USER200_1", "팬 모드 마이페이지에 성공했습니다."),
    FAN_PERFORMANCE_HISTORY_GET_SUCCESS(OK, "USER200_2", "공연 참여 기록을 조회했습니다."),
    FAN_PERFORMANCE_INTEREST_GET_SUCCESS(OK, "USER200_3", "관심 공연 목록을 조회했습니다."),
    USER_BLOCK_SUCCESS(OK, "USER200_4", "사용자를 차단했습니다.");

    private final int status;
    private final String code;
    private final String message;
}
