package com.umc.bscene.domain.chat.dto.response;

import com.umc.bscene.domain.chat.entity.ChatMessage;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record ChatMessageDetailResponse(
        Long chatMessageId,
        Long senderUserId,
        String senderName,
        String content,
        boolean isMine,
        boolean isRead,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt
) {
    public static ChatMessageDetailResponse from(ChatMessage message, Long viewerId) {
        boolean mine = message.getSender().getId().equals(viewerId);
        return new ChatMessageDetailResponse(
                message.getChatMessageId(), message.getSender().getId(),
                mine ? "나" : message.getSender().getName(), message.getContent(),
                mine, message.getReadAt() != null, message.getCreatedAt());
    }
}
