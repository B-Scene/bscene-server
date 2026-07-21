package com.umc.bscene.domain.chat.response.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LiveChatWebSocketErrorCode implements BaseResponseCode {
    INVALID_FRAME(400, "LIVE_CHAT_INVALID_FRAME", "라이브 채팅 요청 형식이 올바르지 않습니다."),
    EMPTY_CONTENT(400, "LIVE_CHAT_CONTENT_EMPTY", "채팅 내용을 입력해주세요."),
    CONTENT_TOO_LONG(400, "LIVE_CHAT_CONTENT_TOO_LONG", "채팅은 최대 500자까지 입력할 수 있습니다."),
    LIVE_NOT_OPEN(404, "LIVE_CHAT_NOT_OPEN", "현재 방송 중인 라이브가 아닙니다."),
    UNSUPPORTED_TYPE(400, "LIVE_CHAT_UNSUPPORTED_TYPE", "지원하지 않는 라이브 채팅 메시지 형식입니다."),
    INTERNAL_ERROR(500, "LIVE_CHAT_INTERNAL_ERROR", "라이브 채팅 처리 중 오류가 발생했습니다.");

    private final int status;
    private final String code;
    private final String message;
}
