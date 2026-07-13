package com.umc.bscene.domain.chat.dto.response;

public record ChatWebSocketTicketResponse(
        String ticket,
        String subprotocol,
        long expiresIn
) {
}
