package com.umc.bscene.domain.chat.dto.response;

public record ChatWebSocketErrorData(
        String code,
        String message
) {
}
