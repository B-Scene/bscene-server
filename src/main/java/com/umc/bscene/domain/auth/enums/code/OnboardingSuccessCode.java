package com.umc.bscene.domain.auth.enums.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.umc.bscene.global.constant.StaticValue.OK;

@Getter
@RequiredArgsConstructor
public enum OnboardingSuccessCode implements BaseResponseCode {

    GENRES_GET_SUCCESS(OK, "GENRE200_1", "장르 목록 조회에 성공했습니다."),
    REGIONS_GET_SUCCESS(OK, "REGION200_1", "지역 목록 조회에 성공했습니다."),

    ONBOARDING_STATUS_GET_SUCCESS(OK, "ONBOARDING200_1", "온보딩 상태 조회에 성공했습니다."),
    FAN_NICKNAME_CHECK_SUCCESS(OK, "ONBOARDING200_2", "팬 닉네임 중복 확인에 성공했습니다."),
    ONBOARDING_SAVE_SUCCESS(OK, "ONBOARDING200_3", "온보딩 정보 저장에 성공했습니다.");


    private final int status;
    private final String code;
    private final String message;
}