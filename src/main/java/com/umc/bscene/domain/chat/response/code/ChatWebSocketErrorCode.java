package com.umc.bscene.domain.chat.response.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ChatWebSocketErrorCode implements BaseResponseCode {
    INVALID_FRAME(HttpStatus.BAD_REQUEST.value(), "DM_INVALID_FRAME", "쪽지 요청 형식이 올바르지 않습니다."),
    EMPTY_CONTENT(HttpStatus.BAD_REQUEST.value(), "DM_CONTENT_EMPTY", "쪽지 내용을 입력해주세요."),
    CONTENT_TOO_LONG(HttpStatus.BAD_REQUEST.value(), "DM_CONTENT_TOO_LONG", "쪽지는 최대 2,000자까지 입력할 수 있습니다."),
    SEND_NOT_ALLOWED(HttpStatus.FORBIDDEN.value(), "DM_SEND_NOT_ALLOWED", "현재 이 채팅방에 쪽지를 보낼 수 없습니다."),
    READ_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "DM_READ_MESSAGE_NOT_FOUND", "읽음 처리할 쪽지를 찾을 수 없습니다.");

    private final int status;
    private final String code;
    private final String message;
}
