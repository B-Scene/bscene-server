package com.umc.bscene.domain.session.enums.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SessionSuccessCode implements BaseResponseCode {

    SESSION_BASIC_PROFILE_GET_SUCCESS(
            HttpStatus.OK.value(),
            "SESSION_BASIC_PROFILE_GET_SUCCESS",
            "세션 기본 정보 조회에 성공했습니다."
    ),
    SESSION_BASIC_PROFILE_UPDATE_SUCCESS(
            HttpStatus.OK.value(),
            "SESSION_BASIC_PROFILE_UPDATE_SUCCESS",
            "세션 기본 정보 수정에 성공했습니다."
    ),

    MY_SESSION_APPLICATION_DETAIL_SUCCESS(
            HttpStatus.OK.value(),
            "MY_SESSION_APPLICATION_DETAIL_SUCCESS",
            "내 지원서 상세 조회에 성공했습니다."
    ),

    MY_SESSION_APPLICATION_UPDATE_SUCCESS(
            HttpStatus.OK.value(),
            "MY_SESSION_APPLICATION_UPDATE_SUCCESS",
            "내 지원서 저장에 성공했습니다."
    ),
    MY_SESSION_APPLICATION_DELETE_SUCCESS(
            HttpStatus.OK.value(),
            "MY_SESSION_APPLICATION_DELETE_SUCCESS",
            "내 지원서 삭제에 성공했습니다."
    ),
    MY_SESSION_APPLICATION_VISIBILITY_UPDATE_SUCCESS(
            HttpStatus.OK.value(),
            "MY_SESSION_APPLICATION_VISIBILITY_UPDATE_SUCCESS",
            "기본 지원서 공개 설정 변경에 성공했습니다."
    ),

    MY_SESSION_APPLICATION_CREATE_SUCCESS(
            HttpStatus.CREATED.value(),
            "MY_SESSION_APPLICATION_CREATE_SUCCESS",
            "지원서 생성에 성공했습니다."
    ),
    MY_SESSION_APPLICATION_SUMMARY_SUCCESS(
            HttpStatus.OK.value(),
            "MY_SESSION_APPLICATION_SUMMARY_SUCCESS",
            "내 세션 지원서 요약 조회에 성공했습니다."
    ),
    SESSION_APPLICATION_SEARCH_SUCCESS(
            HttpStatus.OK.value(),
            "SESSION_APPLICATION_SEARCH_SUCCESS",
            "세션 찾기 조회에 성공했습니다."
    ),
    SESSION_APPLICATION_DETAIL_SUCCESS(
            HttpStatus.OK.value(),
            "SESSION_APPLICATION_DETAIL_SUCCESS",
            "세션 지원서 상세 조회에 성공했습니다."
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
    ),
    SESSION_RECRUITMENT_INTEREST_SET_SUCCESS(
            HttpStatus.OK.value(),
            "SESSION_RECRUITMENT_INTEREST_SET_SUCCESS",
            "세션 모집 공고 찜에 성공했습니다."
    ),
    SESSION_RECRUITMENT_INTEREST_UNSET_SUCCESS(
            HttpStatus.OK.value(),
            "SESSION_RECRUITMENT_INTEREST_UNSET_SUCCESS",
            "세션 모집 공고 찜 취소에 성공했습니다."
    ),
    SESSION_APPLICATION_SUBMIT_SUCCESS(
            HttpStatus.CREATED.value(),
            "SESSION_APPLICATION_SUBMIT_SUCCESS",
            "지원이 완료되었습니다."
    ),
    MY_APPLICATION_SUBMISSION_LIST_SUCCESS(
            HttpStatus.OK.value(),
            "MY_APPLICATION_SUBMISSION_LIST_SUCCESS",
            "내 지원 내역 조회에 성공했습니다."
    ),
    APPLICATION_SUBMISSION_CANCEL_SUCCESS(
            HttpStatus.OK.value(),
            "APPLICATION_SUBMISSION_CANCEL_SUCCESS",
            "지원이 취소되었습니다."
    ),
    SUBMITTED_APPLICATION_DETAIL_SUCCESS(
            HttpStatus.OK.value(),
            "SUBMITTED_APPLICATION_DETAIL_SUCCESS",
            "제출된 지원서 조회에 성공했습니다."
    ),
    SESSION_RECRUITMENT_INTEREST_LIST_SUCCESS(
            HttpStatus.OK.value(),
            "SESSION_RECRUITMENT_INTEREST_LIST_SUCCESS",
            "관심 공고 조회에 성공했습니다."
    ),
    SESSION_RECRUITMENT_RECENT_LIST_SUCCESS(
            HttpStatus.OK.value(),
            "SESSION_RECRUITMENT_RECENT_LIST_SUCCESS",
            "최근 본 공고 조회에 성공했습니다."
    ),
    SESSION_RECRUITMENT_SEARCH_HISTORY_SUCCESS(
            HttpStatus.OK.value(),
            "SESSION_RECRUITMENT_SEARCH_HISTORY_SUCCESS",
            "세션 최근검색어 조회에 성공했습니다."
    ),
    SESSION_RECRUITMENT_SEARCH_HISTORY_DELETE_SUCCESS(
            HttpStatus.OK.value(),
            "SESSION_RECRUITMENT_SEARCH_HISTORY_DELETE_SUCCESS",
            "세션 최근검색어 삭제에 성공했습니다."
    );

    private final int status;
    private final String code;
    private final String message;
}
