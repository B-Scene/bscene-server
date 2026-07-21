package com.umc.bscene.domain.chat.response.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter @RequiredArgsConstructor
public enum ChatErrorCode implements BaseResponseCode {
    USER_BLOCKED(HttpStatus.FORBIDDEN.value(), "CHAT_USER_BLOCKED", "차단 관계인 사용자와 쪽지를 시작할 수 없습니다."),
    INVALID_CHAT_CONTEXT(HttpStatus.BAD_REQUEST.value(), "INVALID_CHAT_CONTEXT", "채팅 시작 정보를 확인해주세요."),
    CHAT_TARGET_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "CHAT_TARGET_NOT_FOUND", "쪽지를 보낼 대상을 찾을 수 없습니다."),
    SELF_CHAT_NOT_ALLOWED(HttpStatus.BAD_REQUEST.value(), "SELF_CHAT_NOT_ALLOWED", "본인에게 쪽지를 보낼 수 없습니다."),
    PUBLIC_SESSION_PROFILE_REQUIRED(HttpStatus.CONFLICT.value(), "PUBLIC_SESSION_PROFILE_REQUIRED", "공개 상태의 기본 지원서를 먼저 생성해주세요."),
    REJECTED_APPLICATION_CHAT_NOT_ALLOWED(HttpStatus.FORBIDDEN.value(), "REJECTED_APPLICATION_CHAT_NOT_ALLOWED", "지원이 거절된 공고에서는 쪽지를 보낼 수 없습니다."),
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "CHAT_ROOM_NOT_FOUND", "해당 쪽지방을 찾을 수 없습니다."),
    CHAT_ROOM_ACCESS_DENIED(HttpStatus.FORBIDDEN.value(), "CHAT_ROOM_ACCESS_DENIED", "해당 쪽지방에 접근할 권한이 없습니다.");
    private final int status;
    private final String code;
    private final String message;
}
