package com.umc.bscene.domain.session.enums.code.error;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.umc.bscene.global.constant.StaticValue.BAD_REQUEST;
import static com.umc.bscene.global.constant.StaticValue.CONFLICT;
import static com.umc.bscene.global.constant.StaticValue.FORBIDDEN;
import static com.umc.bscene.global.constant.StaticValue.NOT_FOUND;

@Getter
@RequiredArgsConstructor
public enum SessionErrorCode implements BaseResponseCode {

    INVALID_SESSION_BASIC_PROFILE_REQUEST("SESSION400_1", BAD_REQUEST, "세션 기본 정보 입력값을 확인해주세요."),
    INVALID_SESSION_APPLICATION_REQUEST("SESSION400_2", BAD_REQUEST, "지원서 필수 입력값을 확인해주세요."),
    SESSION_APPLICATION_VISIBILITY_NOT_ALLOWED("SESSION400_3", BAD_REQUEST, "기본 지원서만 공개 여부를 변경할 수 있습니다."),
    INVALID_SESSION_RECRUITMENT_REQUEST("SESSION400_4", BAD_REQUEST, "세션 모집 공고 필수 입력값을 확인해주세요."),
    INVALID_SESSION_RECRUITMENT_DEADLINE("SESSION400_5", BAD_REQUEST, "모집 마감일은 현재 시간 이후여야 합니다."),
    INVALID_SESSION_RECRUITMENT_FILTER("SESSION400_6", BAD_REQUEST, "유효하지 않은 세션 모집 공고 필터값입니다."),
    FIRST_SESSION_APPLICATION_MUST_BE_DEFAULT("SESSION400_7", BAD_REQUEST, "첫 지원서는 기본 지원서로 생성해야 합니다."),
    SESSION_RECRUITMENT_APPLICATION_CLOSED("SESSION400_8", BAD_REQUEST, "지원이 마감된 공고입니다."),
    BAND_PERMISSION_DENIED("SESSION403_1", FORBIDDEN, "밴드 오너만 세션 모집 공고를 등록, 수정, 삭제할 수 있습니다."),
    SELF_RECRUITMENT_APPLICATION_NOT_ALLOWED("SESSION403_2", FORBIDDEN, "본인이 만든 공고에는 지원할 수 없습니다."),
    SELF_APPLICATION_DECISION_NOT_ALLOWED("SESSION403_3", FORBIDDEN, "본인의 지원 건은 본인이 수락하거나 거절할 수 없습니다."),
    SESSION_APPLICATION_NOT_FOUND("SESSION404_1", NOT_FOUND, "지원서를 찾을 수 없습니다."),
    SESSION_RECRUITMENT_NOT_FOUND("SESSION404_2", NOT_FOUND, "세션 모집 공고를 찾을 수 없습니다."),
    APPLICATION_SUBMISSION_NOT_FOUND("SESSION404_3", NOT_FOUND, "지원 내역을 찾을 수 없습니다."),
    SESSION_RECRUITMENT_INTEREST_ALREADY_EXISTS("SESSION409_1", CONFLICT, "이미 찜한 세션 모집 공고입니다."),
    DEFAULT_SESSION_APPLICATION_ALREADY_EXISTS("SESSION409_2", CONFLICT, "기본 지원서는 하나만 생성할 수 있습니다."),
    SESSION_APPLICATION_ALREADY_SUBMITTED("SESSION409_3", CONFLICT, "이미 해당 지원서로 지원한 공고입니다."),
    APPLICATION_SUBMISSION_CANCEL_NOT_ALLOWED("SESSION409_4", CONFLICT, "진행 중인 지원만 취소할 수 있습니다."),
    APPLICATION_SUBMISSION_ALREADY_PROCESSED("SESSION409_5", CONFLICT, "이미 처리되었거나 취소된 지원입니다."),
    APPLICATION_SUBMISSION_NOT_CONFIRMABLE("SESSION409_6", CONFLICT, "지원자 최종 확정이 가능한 상태가 아닙니다."),
    ;

    private final String code;
    private final int status;
    private final String message;
}
