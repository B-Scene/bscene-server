package com.umc.bscene.domain.chat.dto.request;

public record ChatMessageReadRequest(
        Long chatRoomId,
        Long lastReadMessageId
) {
}
