package com.umc.bscene.domain.performance.response.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.umc.bscene.global.constant.StaticValue.CREATED;
import static com.umc.bscene.global.constant.StaticValue.OK;

@Getter
@RequiredArgsConstructor
public enum PerformanceSuccessCode implements BaseResponseCode {

    PERFORMANCE_CREATE_SUCCESS(CREATED, "SHOW201_1", "공연이 등록됐어요."),
    PERFORMANCE_ALARM_SET_SUCCESS(CREATED, "SHOW201_2", "공연 알림을 설정했어요."),
    PERFORMANCE_INTEREST_SET_SUCCESS(CREATED, "SHOW201_3", "관심 공연으로 등록했어요."),

    PERFORMANCE_LIST_GET_SUCCESS(OK, "SHOW200_1", "공연 목록을 조회했습니다."),
    PERFORMANCE_DETAIL_GET_SUCCESS(OK, "SHOW200_2", "공연 상세를 조회했습니다."),
    PERFORMANCE_UPDATE_SUCCESS(OK, "SHOW200_3", "공연 정보가 수정됐어요."),
    PERFORMANCE_DELETE_SUCCESS(OK, "SHOW200_4", "공연이 삭제됐어요."),
    PERFORMANCE_ALARM_UNSET_SUCCESS(OK, "SHOW200_5", "공연 알림을 해제했어요."),
    PERFORMANCE_INTEREST_UNSET_SUCCESS(OK, "SHOW200_6", "관심 공연을 해제했어요."),
    PERFORMANCE_PARTICIPATION_COMPLETE_SUCCESS(OK, "SHOW200_7", "공연 참여를 완료했어요.");

    private final int status;
    private final String code;
    private final String message;
}
