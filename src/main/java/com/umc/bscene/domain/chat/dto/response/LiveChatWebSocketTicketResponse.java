package com.umc.bscene.domain.chat.dto.response;

public record LiveChatWebSocketTicketResponse(
        String ticket,
        String subprotocol,
        long expiresInSeconds
) {
}
