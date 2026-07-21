package com.umc.bscene.domain.chat.dto.request;

public record ChatMessageSendRequest(
        Long chatRoomId,
        String content
) {
}
