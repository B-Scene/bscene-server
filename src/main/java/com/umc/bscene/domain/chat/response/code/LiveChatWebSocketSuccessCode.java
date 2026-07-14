package com.umc.bscene.domain.chat.response.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LiveChatWebSocketSuccessCode implements BaseResponseCode {
    TICKET_ISSUE_SUCCESS(200, "LIVE_CHAT200_1", "라이브 채팅 연결 티켓 발급에 성공했습니다.");

    private final int status;
    private final String code;
    private final String message;
}
