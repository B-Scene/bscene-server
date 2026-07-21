package com.umc.bscene.domain.chat.dto.response;

public record ChatMessageSendResult(
        Long recipientId,
        ChatMessagePushData message
) {
}
