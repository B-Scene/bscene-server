package com.umc.bscene.domain.post.response.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.umc.bscene.global.constant.StaticValue.OK;

@Getter
@RequiredArgsConstructor
public enum PostSuccessCode implements BaseResponseCode {

    POST_CREATE_SUCCESS(OK, "POST201_1", "콘텐츠가 업로드됐어요."),

    POST_LIST_GET_SUCCESS(OK, "POST200_1", "콘텐츠 목록을 조회했습니다."),
    POST_DETAIL_GET_SUCCESS(OK, "POST200_2", "콘텐츠를 조회했습니다."),
    POST_UPDATE_SUCCESS(OK, "POST201_2", "콘텐츠가 수정됐어요."),
    POST_DELETE_SUCCESS(OK, "POST201_3", "콘텐츠가 삭제됐어요."),

    POST_DETAIL_PAGE_GET_SUCCESS(OK, "POST200_3", "콘텐츠 상세페이지를 조회했습니다."),
    POST_COMMENT_LIST_GET_SUCCESS(OK, "POST200_4", "댓글 목록을 조회했습니다."),
    POST_COMMENT_CREATE_SUCCESS(OK, "POST200_5", "댓글을 등록했어요."),
    POST_COMMENT_UPDATE_SUCCESS(OK, "POST200_6", "댓글을 수정했어요."),
    POST_COMMENT_DELETE_SUCCESS(OK, "POST200_7", "댓글을 삭제했어요."),
    POST_LIKE_SET_SUCCESS(OK, "POST200_8", "좋아요를 등록했어요."),
    POST_LIKE_UNSET_SUCCESS(OK, "POST200_9", "좋아요를 해제했어요.");

    private final int status;
    private final String code;
    private final String message;
}
