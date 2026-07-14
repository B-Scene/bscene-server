package com.umc.bscene.domain.chat.dto.response;

public record LiveChatMessageData(
        String messageId,
        Long liveId,
        Long senderId,
        String senderName,
        String content,
        String sentAt
) {
}
