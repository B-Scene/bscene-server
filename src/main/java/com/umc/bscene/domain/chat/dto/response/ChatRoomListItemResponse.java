package com.umc.bscene.domain.chat.dto.response;

import com.umc.bscene.domain.chat.entity.ChatMessage;
import com.umc.bscene.domain.chat.entity.ChatRoom;
import com.umc.bscene.domain.chat.enums.ChatContextType;
import com.umc.bscene.domain.session.entity.SessionApplication;

import java.time.LocalDateTime;

public record ChatRoomListItemResponse(
        Long chatRoomId,
        ChatContextType contextType,
        Long contextId,
        Long counterpartUserId,
        String counterpartName,
        String counterpartProfileImageUrl,
        String part,
        String lastMessage,
        LocalDateTime lastMessageAt,
        long unreadCount,
        boolean canSend
) {
    public static ChatRoomListItemResponse of(ChatRoom room, Long viewerId,
                                               ChatMessage lastMessage, long unreadCount,
                                               boolean canSend,
                                               SessionApplication counterpartApplication,
                                               String sessionProfileImageUrl) {
        boolean viewerIsSender = room.getSender().getId().equals(viewerId);
        var counterpart = viewerIsSender ? room.getRecipient() : room.getSender();
        if (room.getContextType() == ChatContextType.RECRUITMENT) {
            var recruitment = room.getSessionRecruitment();
            String name = viewerIsSender ? recruitment.getBand().getName() : room.getSender().getName();
            String profileImageUrl = viewerIsSender
                    ? recruitment.getBand().getProfileImageUrl()
                    : sessionProfileImageUrl;
            String part = viewerIsSender
                    ? recruitment.getPart().getDescription()
                    : counterpartApplication.getPart().getDescription();
            return new ChatRoomListItemResponse(room.getChatRoomId(), room.getContextType(),
                    recruitment.getSessionRecruitmentId(), counterpart.getId(), name,
                    profileImageUrl,
                    part,
                    lastMessage == null ? null : lastMessage.getContent(),
                    lastMessage == null ? null : lastMessage.getCreatedAt(), unreadCount, canSend);
        }
        var application = counterpartApplication;
        return new ChatRoomListItemResponse(room.getChatRoomId(), room.getContextType(),
                application.getSessionApplicationId(), counterpart.getId(), counterpart.getName(),
                sessionProfileImageUrl,
                application.getPart().getDescription(),
                lastMessage == null ? null : lastMessage.getContent(),
                lastMessage == null ? null : lastMessage.getCreatedAt(), unreadCount, canSend);
    }
}
