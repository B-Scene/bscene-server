package com.umc.bscene.domain.auth.enums.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.umc.bscene.global.constant.StaticValue.*;

@Getter
@RequiredArgsConstructor
public enum OnboardingErrorCode implements BaseResponseCode {

    INVALID_CURRENT_MODE(BAD_REQUEST, "ONBOARDING400_1", "현재 모드는 사용 가능 모드 중 하나여야 합니다."),
    FAN_NICKNAME_REQUIRED(BAD_REQUEST, "ONBOARDING400_2", "팬 모드는 닉네임이 필요합니다."),
    GENRE_REQUIRED(BAD_REQUEST, "ONBOARDING400_3", "팬 모드는 관심 장르가 필요합니다."),
    REGION_REQUIRED(BAD_REQUEST, "ONBOARDING400_4", "팬 모드는 활동 지역이 필요합니다."),
    USER_NOT_FOUND(NOT_FOUND, "ONBOARDING404_1", "존재하지 않는 회원입니다."),
    DUPLICATE_FAN_NICKNAME(CONFLICT, "ONBOARDING409_1", "이미 사용 중인 닉네임입니다."),
    ALREADY_ONBOARDED(CONFLICT, "ONBOARDING409_2", "이미 온보딩을 완료한 회원입니다.");

    private final int status;
    private final String code;
    private final String message;
}
