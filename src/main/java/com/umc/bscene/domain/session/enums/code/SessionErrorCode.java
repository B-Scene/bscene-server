package com.umc.bscene.domain.session.enums.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SessionErrorCode implements BaseResponseCode {

    INVALID_SESSION_BASIC_PROFILE_REQUEST(
            HttpStatus.BAD_REQUEST.value(),
            "INVALID_SESSION_BASIC_PROFILE_REQUEST",
            "세션 기본 정보 입력값을 확인해주세요."
    ),

    SESSION_APPLICATION_NOT_FOUND(
            HttpStatus.NOT_FOUND.value(),
            "SESSION_APPLICATION_NOT_FOUND",
            "지원서를 찾을 수 없습니다."
    ),
    SESSION_RECRUITMENT_NOT_FOUND(
            HttpStatus.NOT_FOUND.value(),
            "SESSION_RECRUITMENT_NOT_FOUND",
            "세션 모집 공고를 찾을 수 없습니다."
    ),

    INVALID_SESSION_APPLICATION_REQUEST(
            HttpStatus.BAD_REQUEST.value(),
            "INVALID_SESSION_APPLICATION_REQUEST",
            "지원서 필수 입력값을 확인해주세요."
    ),
    SESSION_APPLICATION_VISIBILITY_NOT_ALLOWED(
            HttpStatus.BAD_REQUEST.value(),
            "SESSION_APPLICATION_VISIBILITY_NOT_ALLOWED",
            "기본 지원서만 공개 여부를 변경할 수 있습니다."
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
            "밴드 오너만 세션 모집 공고를 등록, 수정, 삭제할 수 있습니다."
    ),
    SESSION_RECRUITMENT_INTEREST_ALREADY_EXISTS(
            HttpStatus.CONFLICT.value(),
            "SESSION_RECRUITMENT_INTEREST_ALREADY_EXISTS",
            "이미 찜한 세션 모집 공고입니다."
    ),
    DEFAULT_SESSION_APPLICATION_ALREADY_EXISTS(
            HttpStatus.CONFLICT.value(),
            "DEFAULT_SESSION_APPLICATION_ALREADY_EXISTS",
            "기본 지원서는 하나만 생성할 수 있습니다."
    ),
    SESSION_APPLICATION_ALREADY_SUBMITTED(
            HttpStatus.CONFLICT.value(),
            "SESSION_APPLICATION_ALREADY_SUBMITTED",
            "이미 해당 지원서로 지원한 공고입니다."
    ),
    SESSION_RECRUITMENT_APPLICATION_CLOSED(
            HttpStatus.BAD_REQUEST.value(),
            "SESSION_RECRUITMENT_APPLICATION_CLOSED",
            "지원이 마감된 공고입니다."
    ),
    SELF_RECRUITMENT_APPLICATION_NOT_ALLOWED(
            HttpStatus.FORBIDDEN.value(),
            "SELF_RECRUITMENT_APPLICATION_NOT_ALLOWED",
            "본인이 만든 공고에는 지원할 수 없습니다."
    ),
    APPLICATION_SUBMISSION_NOT_FOUND(
            HttpStatus.NOT_FOUND.value(),
            "APPLICATION_SUBMISSION_NOT_FOUND",
            "지원 내역을 찾을 수 없습니다."
    ),
    APPLICATION_SUBMISSION_CANCEL_NOT_ALLOWED(
            HttpStatus.CONFLICT.value(),
            "APPLICATION_SUBMISSION_CANCEL_NOT_ALLOWED",
            "진행 중인 지원만 취소할 수 있습니다."
    );

    private final int status;
    private final String code;
    private final String message;
}
