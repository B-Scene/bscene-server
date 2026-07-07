package com.umc.bscene.domain.post.response.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.umc.bscene.global.constant.StaticValue.OK;

@Getter
@RequiredArgsConstructor
public enum PostSuccessCode implements BaseResponseCode {

    POST_CREATE_SUCCESS(OK, "POST2011", "콘텐츠가 업로드됐어요."),

    POST_LIST_GET_SUCCESS(OK, "POST2001", "콘텐츠 목록을 조회했습니다."),
    POST_DETAIL_GET_SUCCESS(OK, "POST2002", "콘텐츠를 조회했습니다."),
    POST_UPDATE_SUCCESS(OK, "POST2012", "콘텐츠가 수정됐어요."),
    POST_DELETE_SUCCESS(OK, "POST2013", "콘텐츠가 삭제됐어요.");

    private final int status;
    private final String code;
    private final String message;
}
