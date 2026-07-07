package com.umc.bscene.domain.session.enums.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SessionProfileSuccessCode implements BaseResponseCode {

    MY_SESSION_PROFILE_GET_SUCCESS(
            HttpStatus.OK.value(),
            "MY_SESSION_PROFILE_GET_SUCCESS",
            "내 세션 프로필 조회에 성공했습니다."
    ),

    MY_SESSION_PROFILE_EMPTY(
            HttpStatus.OK.value(),
            "MY_SESSION_PROFILE_EMPTY",
            "등록된 세션 프로필이 없습니다."
    ),

    MY_SESSION_PROFILE_UPDATE_SUCCESS(
            HttpStatus.OK.value(),
            "MY_SESSION_PROFILE_UPDATE_SUCCESS",
            "내 세션 프로필 저장에 성공했습니다."
    ),

    SESSION_RECRUITMENT_CREATE_SUCCESS(
            HttpStatus.CREATED.value(),
            "SESSION_RECRUITMENT_CREATE_SUCCESS",
            "세션 모집 공고 등록에 성공했습니다."
    );

    private final int status;
    private final String code;
    private final String message;
}