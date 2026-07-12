package com.umc.bscene.domain.chat.response.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter @RequiredArgsConstructor
public enum ChatSuccessCode implements BaseResponseCode {
    CHAT_ROOM_CREATE_SUCCESS(HttpStatus.CREATED.value(), "CHAT_ROOM_CREATE_SUCCESS", "채팅방 생성에 성공했습니다.");
    private final int status;
    private final String code;
    private final String message;
}
