package com.umc.bscene.domain.onboarding.response.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.umc.bscene.global.constant.StaticValue.OK;

@Getter
@RequiredArgsConstructor
public enum OnboardingSuccessCode implements BaseResponseCode {

    GENRES_GET_SUCCESS(OK, "GENRE200_1", "장르 목록 조회에 성공했습니다."),
    ONBOARDING_STATUS_GET_SUCCESS(OK, "ONBOARDING200_1", "온보딩 상태 조회에 성공했습니다.");

    private final int status;
    private final String code;
    private final String message;
}