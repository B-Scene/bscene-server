package com.umc.bscene.domain.fanhome.response.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.umc.bscene.global.constant.StaticValue.OK;

@Getter
@RequiredArgsConstructor
public enum FanHomeSuccessCode implements BaseResponseCode {

    FAN_HOME_GET_SUCCESS(OK, "HOME200_1", "팬 홈을 조회했습니다."),
    FOLLOWING_BAND_NEWS_GET_SUCCESS(OK, "HOME200_2", "팔로우한 밴드 소식을 조회했습니다."),
    UPCOMING_PERFORMANCE_GET_SUCCESS(OK, "HOME200_3", "다가오는 공연을 조회했습니다.");

    private final int status;
    private final String code;
    private final String message;
}
