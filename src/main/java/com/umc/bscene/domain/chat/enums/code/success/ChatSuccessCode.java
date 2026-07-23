package com.umc.bscene.domain.chat.enums.code.success;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.umc.bscene.global.constant.StaticValue.CREATED;
import static com.umc.bscene.global.constant.StaticValue.OK;

@Getter
@RequiredArgsConstructor
public enum ChatSuccessCode implements BaseResponseCode {

    CHAT_ROOM_LIST_SUCCESS("CHAT200_1", OK, "쪽지함 조회에 성공했습니다."),
    CHAT_ROOM_DETAIL_SUCCESS("CHAT200_2", OK, "쪽지 상세 조회에 성공했습니다."),
    CHAT_ROOM_LEAVE_SUCCESS("CHAT200_3", OK, "채팅방 나가기에 성공했습니다."),
    DM_TICKET_ISSUE_SUCCESS("CHAT200_4", OK, "쪽지 웹소켓 연결 티켓 발급에 성공했습니다."),
    LIVE_CHAT_TICKET_ISSUE_SUCCESS("LIVE200_16", OK, "라이브 채팅 연결 티켓 발급에 성공했습니다."),
    CHAT_ROOM_CREATE_SUCCESS("CHAT201_1", CREATED, "채팅방 생성에 성공했습니다."),
    ;

    private final String code;
    private final int status;
    private final String message;
}
