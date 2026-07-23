package com.umc.bscene.domain.session.enums.code.success;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.umc.bscene.global.constant.StaticValue.CREATED;
import static com.umc.bscene.global.constant.StaticValue.OK;

@Getter
@RequiredArgsConstructor
public enum SessionSuccessCode implements BaseResponseCode {

    SESSION_BASIC_PROFILE_GET_SUCCESS("SESSION200_1", OK, "세션 기본 정보 조회에 성공했습니다."),
    SESSION_BASIC_PROFILE_UPDATE_SUCCESS("SESSION200_2", OK, "세션 기본 정보 수정에 성공했습니다."),
    MY_SESSION_APPLICATION_DETAIL_SUCCESS("SESSION200_3", OK, "내 지원서 상세 조회에 성공했습니다."),
    MY_SESSION_APPLICATION_UPDATE_SUCCESS("SESSION200_4", OK, "내 지원서 저장에 성공했습니다."),
    MY_SESSION_APPLICATION_DELETE_SUCCESS("SESSION200_5", OK, "내 지원서 삭제에 성공했습니다."),
    MY_SESSION_APPLICATION_VISIBILITY_UPDATE_SUCCESS("SESSION200_6", OK, "기본 지원서 공개 설정 변경에 성공했습니다."),
    MY_SESSION_APPLICATION_SUMMARY_SUCCESS("SESSION200_7", OK, "내 세션 지원서 요약 조회에 성공했습니다."),
    SESSION_APPLICATION_SEARCH_SUCCESS("SESSION200_8", OK, "세션 찾기 조회에 성공했습니다."),
    SESSION_APPLICATION_DETAIL_SUCCESS("SESSION200_9", OK, "세션 지원서 상세 조회에 성공했습니다."),
    SESSION_RECRUITMENT_LIST_SUCCESS("SESSION200_10", OK, "세션 모집 공고 목록 조회에 성공했습니다."),
    SESSION_RECRUITMENT_UPDATE_SUCCESS("SESSION200_11", OK, "세션 모집 공고 수정에 성공했습니다."),
    SESSION_RECRUITMENT_DELETE_SUCCESS("SESSION200_12", OK, "세션 모집 공고 삭제에 성공했습니다."),
    SESSION_RECRUITMENT_DETAIL_SUCCESS("SESSION200_13", OK, "세션 모집 공고 상세 조회에 성공했습니다."),
    SESSION_RECRUITMENT_INTEREST_SET_SUCCESS("SESSION200_14", OK, "세션 모집 공고 찜에 성공했습니다."),
    SESSION_RECRUITMENT_INTEREST_UNSET_SUCCESS("SESSION200_15", OK, "세션 모집 공고 찜 취소에 성공했습니다."),
    MY_APPLICATION_SUBMISSION_LIST_SUCCESS("SESSION200_16", OK, "내 지원 내역 조회에 성공했습니다."),
    APPLICATION_SUBMISSION_CANCEL_SUCCESS("SESSION200_17", OK, "지원이 취소되었습니다."),
    SUBMITTED_APPLICATION_DETAIL_SUCCESS("SESSION200_18", OK, "제출된 지원서 조회에 성공했습니다."),
    SESSION_RECRUITMENT_INTEREST_LIST_SUCCESS("SESSION200_19", OK, "관심 공고 조회에 성공했습니다."),
    SESSION_RECRUITMENT_RECENT_LIST_SUCCESS("SESSION200_20", OK, "최근 본 공고 조회에 성공했습니다."),
    SESSION_RECRUITMENT_SEARCH_HISTORY_SUCCESS("SESSION200_21", OK, "세션 최근검색어 조회에 성공했습니다."),
    SESSION_RECRUITMENT_SEARCH_HISTORY_DELETE_SUCCESS("SESSION200_22", OK, "세션 최근검색어 삭제에 성공했습니다."),
    MY_SESSION_APPLICATION_CREATE_SUCCESS("SESSION201_1", CREATED, "지원서 생성에 성공했습니다."),
    SESSION_RECRUITMENT_CREATE_SUCCESS("SESSION201_2", CREATED, "세션 모집 공고 등록에 성공했습니다."),
    SESSION_APPLICATION_SUBMIT_SUCCESS("SESSION201_3", CREATED, "지원이 완료되었습니다."),
    SESSION_RECRUITMENT_MANAGEMENT_LIST_SUCCESS("SESSION200_23", OK, "모집 공고 관리 목록 조회에 성공했습니다."),
    ;

    private final String code;
    private final int status;
    private final String message;
}
