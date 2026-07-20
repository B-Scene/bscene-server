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
    public static ChatRoomCreateResponse recruitment(ChatRoom room, boolean created) {
        var recruitment = room.getSessionRecruitment();
        return new ChatRoomCreateResponse(room.getChatRoomId(), room.getContextType(),
                recruitment.getSessionRecruitmentId(), recruitment.getRecruitmentTitle(),
                recruitment.getGenre().getName(), recruitment.getPart().getDescription(),
                room.getRecipient().getId(), recruitment.getBand().getName(), created);
    }

    public static ChatRoomCreateResponse sessionSearch(ChatRoom room, boolean created) {
        var application = room.getSessionApplication();
        return new ChatRoomCreateResponse(room.getChatRoomId(), room.getContextType(),
                application.getSessionApplicationId(), application.getNickname(),
                application.getGenre().getName(), application.getPart().getDescription(),
                room.getRecipient().getId(), application.getNickname(), created);
    }
}
