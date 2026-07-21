package com.umc.bscene.domain.chat.dto.response;

public record ChatMessagePushData(
        Long chatMessageId,
        Long chatRoomId,
        Long senderId,
        String senderName,
        String profileImageUrl,
        String content,
        String readAt,
        String createdAt
) {
}
