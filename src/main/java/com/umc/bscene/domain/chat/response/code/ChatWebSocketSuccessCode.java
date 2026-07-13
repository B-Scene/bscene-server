package com.umc.bscene.domain.chat.response.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ChatWebSocketSuccessCode implements BaseResponseCode {
    TICKET_ISSUE_SUCCESS(
            HttpStatus.OK.value(),
            "DM200_N",
            "쪽지 웹소켓 연결 티켓 발급에 성공했습니다."
    );

    private final int status;
    private final String code;
    private final String message;
}
