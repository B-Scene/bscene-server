package com.umc.bscene.domain.chat.response.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ChatWebSocketSystemErrorCode implements BaseResponseCode {
    UNSUPPORTED_TYPE(HttpStatus.BAD_REQUEST.value(), "DM_UNSUPPORTED_TYPE", "지원하지 않는 쪽지 요청 타입입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR.value(), "DM_INTERNAL_ERROR", "쪽지 처리 중 오류가 발생했습니다.");

    private final int status;
    private final String code;
    private final String message;
}
