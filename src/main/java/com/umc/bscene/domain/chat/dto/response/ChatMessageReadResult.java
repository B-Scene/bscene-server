package com.umc.bscene.domain.chat.dto.response;

public record ChatMessageReadResult(
        Long counterpartId,
        ChatMessageReadPushData read
) {
}
