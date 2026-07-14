package com.umc.bscene.domain.chat.dto.response;

public record ChatWebSocketPushFrame(
        String type,
        Long id,
        Object data,
        String clientMsgId,
        String timeStamp
) {
}
