package com.umc.bscene.domain.session.enums.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SessionErrorCode implements BaseResponseCode {

    SESSION_PROFILE_NOT_FOUND(
            HttpStatus.NOT_FOUND.value(),
            "SESSION_PROFILE_NOT_FOUND",
            "세션 프로필을 찾을 수 없습니다."
    ),
    BAND_PROFILE_NOT_FOUND(
            HttpStatus.NOT_FOUND.value(),
            "BAND_PROFILE_NOT_FOUND",
            "밴드 프로필을 찾을 수 없습니다."
    ),

    SESSION_RECRUITMENT_NOT_FOUND(
            HttpStatus.NOT_FOUND.value(),
            "SESSION_RECRUITMENT_NOT_FOUND",
            "세션 모집 공고를 찾을 수 없습니다."
    ),

    INVALID_SESSION_PROFILE_REQUEST(
            HttpStatus.BAD_REQUEST.value(),
            "INVALID_SESSION_PROFILE_REQUEST",
            "세션 프로필 필수 입력값을 확인해주세요."
    ),

    INVALID_SESSION_RECRUITMENT_REQUEST(
            HttpStatus.BAD_REQUEST.value(),
            "INVALID_SESSION_RECRUITMENT_REQUEST",
            "세션 모집 공고 필수 입력값을 확인해주세요."
    ),

    INVALID_SESSION_RECRUITMENT_DEADLINE(
            HttpStatus.BAD_REQUEST.value(),
            "INVALID_SESSION_RECRUITMENT_DEADLINE",
            "모집 마감일은 현재 시간 이후여야 합니다."
    ),

    INVALID_SESSION_RECRUITMENT_FILTER(
            HttpStatus.BAD_REQUEST.value(),
            "INVALID_SESSION_RECRUITMENT_FILTER",
            "유효하지 않은 세션 모집 공고 필터값입니다."
    ),

    BAND_PERMISSION_DENIED(
            HttpStatus.FORBIDDEN.value(),
            "BAND_PERMISSION_DENIED",
            "세션 모집 공고에 대한 권한이 없습니다."
    );

    private final int status;
    private final String code;
    private final String message;
}