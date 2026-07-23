package com.umc.bscene.domain.chat.dto.response;

import com.umc.bscene.domain.chat.entity.ChatRoom;
import com.umc.bscene.domain.chat.enums.ChatContextType;

public record ChatRoomCreateResponse(
        Long chatRoomId,
        ChatContextType contextType,
        Long contextId,
        String title,
        String genre,
        String part,
        Long recipientUserId,
        String recipientName,
        boolean created
) {
    public static ChatRoomCreateResponse recruitment(
            ChatRoom room, Long viewerId, boolean created) {
        var recruitment = room.getSessionRecruitment();
        boolean viewerIsApplicant = room.getSender().getId().equals(viewerId);
        return new ChatRoomCreateResponse(room.getChatRoomId(), room.getContextType(),
                recruitment.getSessionRecruitmentId(), recruitment.getRecruitmentTitle(),
                recruitment.getGenre().getName(), recruitment.getPart().getDescription(),
                viewerIsApplicant ? room.getRecipient().getId() : room.getSender().getId(),
                viewerIsApplicant ? recruitment.getBand().getName() : room.getSender().getName(),
                created);
    }

    public static ChatRoomCreateResponse sessionSearch(ChatRoom room, boolean created) {
        var application = room.getSessionApplication();
        return new ChatRoomCreateResponse(room.getChatRoomId(), room.getContextType(),
                application.getSessionApplicationId(), application.getNickname(),
                application.getGenre().getName(), application.getPart().getDescription(),
                room.getRecipient().getId(), application.getNickname(), created);
    }
}
