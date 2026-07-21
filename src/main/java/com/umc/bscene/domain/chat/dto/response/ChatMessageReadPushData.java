package com.umc.bscene.domain.chat.dto.response;

public record ChatMessageReadPushData(
        Long chatRoomId,
        Long readerId,
        Long lastReadMessageId,
        String readAt
) {
}
