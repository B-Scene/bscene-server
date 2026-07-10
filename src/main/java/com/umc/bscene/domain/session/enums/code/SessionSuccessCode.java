package com.umc.bscene.domain.session.enums.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SessionSuccessCode implements BaseResponseCode {

    MY_SESSION_APPLICATION_GET_SUCCESS(
            HttpStatus.OK.value(),
            "MY_SESSION_APPLICATION_GET_SUCCESS",
            "내 지원서 조회에 성공했습니다."
    ),

    MY_SESSION_APPLICATION_EMPTY(
            HttpStatus.OK.value(),
            "MY_SESSION_APPLICATION_EMPTY",
            "등록된 지원서가 없습니다."
    ),

    MY_SESSION_APPLICATION_UPDATE_SUCCESS(
            HttpStatus.OK.value(),
            "MY_SESSION_APPLICATION_UPDATE_SUCCESS",
            "내 지원서 저장에 성공했습니다."
    ),

    MY_SESSION_APPLICATION_CREATE_SUCCESS(
            HttpStatus.CREATED.value(),
            "MY_SESSION_APPLICATION_CREATE_SUCCESS",
            "지원서 생성에 성공했습니다."
    ),

    SESSION_RECRUITMENT_CREATE_SUCCESS(
            HttpStatus.CREATED.value(),
            "SESSION_RECRUITMENT_CREATE_SUCCESS",
            "세션 모집 공고 등록에 성공했습니다."
    ),
    SESSION_RECRUITMENT_LIST_SUCCESS(
            HttpStatus.OK.value(),
            "SESSION_RECRUITMENT_LIST_SUCCESS",
            "세션 모집 공고 목록 조회에 성공했습니다."
    ),

    SESSION_RECRUITMENT_UPDATE_SUCCESS(
            HttpStatus.OK.value(),
            "SESSION_RECRUITMENT_UPDATE_SUCCESS",
            "세션 모집 공고 수정에 성공했습니다."
    ),
    SESSION_RECRUITMENT_DELETE_SUCCESS(
            HttpStatus.OK.value(),
            "SESSION_RECRUITMENT_DELETE_SUCCESS",
            "세션 모집 공고 삭제에 성공했습니다."
    ),
    SESSION_RECRUITMENT_DETAIL_SUCCESS(
            HttpStatus.OK.value(),
        "SESSION_RECRUITMENT_DETAIL_SUCCESS",
                "세션 모집 공고 상세 조회에 성공했습니다."
                );

    private final int status;
    private final String code;
    private final String message;
}
