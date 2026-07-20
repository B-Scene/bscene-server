package com.umc.bscene.domain.performance.response.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.umc.bscene.global.constant.StaticValue.*;

@Getter
@RequiredArgsConstructor
public enum PerformanceErrorCode implements BaseResponseCode {

    PAST_DATE_NOT_ALLOWED(BAD_REQUEST, "SHOW400_1", "지난 날짜는 등록할 수 없어요."),

    ALREADY_STARTED_PERFORMANCE(BAD_REQUEST, "SHOW400_2", "이미 시작된 공연은 알림을 설정할 수 없어요."),

    PERFORMANCE_NOT_STARTED(BAD_REQUEST, "SHOW400_3", "아직 시작하지 않은 공연이에요."),

    TAG_LIMIT_EXCEEDED(BAD_REQUEST, "SHOW400_4", "태그는 최대 8개까지 입력할 수 있어요."),

    NOT_PERFORMANCE_BAND_MEMBER(FORBIDDEN, "SHOW403_1", "공연에 대한 권한이 없어요."),

    PERFORMANCE_NOT_FOUND(NOT_FOUND, "SHOW404_1", "존재하지 않는 공연이에요."),

    PARTICIPATION_NOT_FOUND(NOT_FOUND, "SHOW404_2", "알림을 설정한 공연이 아니에요."),

    ALREADY_ALARM_SET(CONFLICT, "SHOW409_1", "이미 알림을 설정한 공연이에요."),

    ALREADY_INTEREST_SET(CONFLICT, "SHOW409_2", "이미 관심 공연으로 등록한 공연이에요."),

    ALREADY_COMPLETED_PARTICIPATION(CONFLICT, "SHOW409_3", "이미 참여 완료한 공연이에요.");

    private final int status;
    private final String code;
    private final String message;
}
