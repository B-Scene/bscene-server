package com.umc.bscene.domain.post.response.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.umc.bscene.global.constant.StaticValue.*;

@Getter
@RequiredArgsConstructor
public enum PostErrorCode implements BaseResponseCode {

    TAG_LIMIT_EXCEEDED(BAD_REQUEST, "POST400_1", "태그는 최대 8개까지 입력할 수 있어요."),
    PHOTO_MEDIA_REQUIRED(BAD_REQUEST, "POST400_2", "사진은 미디어 URL을 1장 이상 입력해주세요."),
    INVALID_VIDEO_MEDIA_COUNT(BAD_REQUEST, "POST400_3", "영상은 미디어 URL을 1개만 입력할 수 있어요."),
    TEXT_MEDIA_NOT_ALLOWED(BAD_REQUEST, "POST400_4", "글 콘텐츠는 미디어 URL을 입력할 수 없어요."),

    NOT_POST_BAND_MEMBER(FORBIDDEN, "POST403_1", "콘텐츠를 수정할 권한이 없어요."),
    NOT_COMMENT_OWNER(FORBIDDEN, "POST403_2", "본인이 작성한 댓글만 수정/삭제할 수 있어요."),

    POST_NOT_FOUND(NOT_FOUND, "POST404_1", "존재하지 않는 콘텐츠예요."),
    COMMENT_NOT_FOUND(NOT_FOUND, "POST404_2", "존재하지 않는 댓글이에요."),

    ALREADY_LIKED(CONFLICT, "POST409_1", "이미 좋아요한 콘텐츠예요.");

    private final int status;
    private final String code;
    private final String message;
}
