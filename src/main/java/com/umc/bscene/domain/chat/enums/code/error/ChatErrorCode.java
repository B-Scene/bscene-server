package com.umc.bscene.domain.chat.enums.code.error;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.umc.bscene.global.constant.StaticValue.BAD_REQUEST;
import static com.umc.bscene.global.constant.StaticValue.FORBIDDEN;
import static com.umc.bscene.global.constant.StaticValue.INTERNAL_SERVER_ERROR;
import static com.umc.bscene.global.constant.StaticValue.NOT_FOUND;

@Getter
@RequiredArgsConstructor
public enum ChatErrorCode implements BaseResponseCode {

    INVALID_CHAT_CONTEXT("CHAT400_1", BAD_REQUEST, "채팅 시작 정보를 확인해주세요."),
    SELF_CHAT_NOT_ALLOWED("CHAT400_2", BAD_REQUEST, "본인에게 쪽지를 보낼 수 없습니다."),
    DM_INVALID_FRAME("CHAT400_3", BAD_REQUEST, "쪽지 요청 형식이 올바르지 않습니다."),
    DM_EMPTY_CONTENT("CHAT400_4", BAD_REQUEST, "쪽지 내용을 입력해주세요."),
    DM_CONTENT_TOO_LONG("CHAT400_5", BAD_REQUEST, "쪽지는 최대 2,000자까지 입력할 수 있습니다."),
    DM_UNSUPPORTED_TYPE("CHAT400_6", BAD_REQUEST, "지원하지 않는 쪽지 요청 타입입니다."),
    USER_BLOCKED("CHAT403_1", FORBIDDEN, "차단 관계인 사용자와 쪽지를 시작할 수 없습니다."),
    REJECTED_APPLICATION_CHAT_NOT_ALLOWED("CHAT403_2", FORBIDDEN, "지원이 거절된 공고에서는 쪽지를 보낼 수 없습니다."),
    CHAT_ROOM_ACCESS_DENIED("CHAT403_3", FORBIDDEN, "해당 쪽지방에 접근할 권한이 없습니다."),
    DM_USER_BLOCKED("CHAT403_4", FORBIDDEN, "차단 관계인 사용자에게는 쪽지를 보낼 수 없습니다."),
    DM_SEND_NOT_ALLOWED("CHAT403_5", FORBIDDEN, "현재 이 채팅방에 쪽지를 보낼 수 없습니다."),
    CHAT_TARGET_NOT_FOUND("CHAT404_1", NOT_FOUND, "쪽지를 보낼 대상을 찾을 수 없습니다."),
    CHAT_ROOM_NOT_FOUND("CHAT404_2", NOT_FOUND, "해당 쪽지방을 찾을 수 없습니다."),
    DM_READ_MESSAGE_NOT_FOUND("CHAT404_3", NOT_FOUND, "읽음 처리할 쪽지를 찾을 수 없습니다."),
    DM_INTERNAL_ERROR("CHAT500_1", INTERNAL_SERVER_ERROR, "쪽지 처리 중 오류가 발생했습니다."),
    LIVE_INVALID_FRAME("LIVE400_5", BAD_REQUEST, "라이브 채팅 요청 형식이 올바르지 않습니다."),
    LIVE_EMPTY_CONTENT("LIVE400_6", BAD_REQUEST, "채팅 내용을 입력해주세요."),
    LIVE_CONTENT_TOO_LONG("LIVE400_7", BAD_REQUEST, "채팅은 최대 500자까지 입력할 수 있습니다."),
    LIVE_UNSUPPORTED_TYPE("LIVE400_8", BAD_REQUEST, "지원하지 않는 라이브 채팅 메시지 형식입니다."),
    LIVE_NOT_OPEN("LIVE404_6", NOT_FOUND, "현재 방송 중인 라이브가 아닙니다."),
    LIVE_INTERNAL_ERROR("LIVE500_1", INTERNAL_SERVER_ERROR, "라이브 채팅 처리 중 오류가 발생했습니다."),
    ;

    private final String code;
    private final int status;
    private final String message;
}
