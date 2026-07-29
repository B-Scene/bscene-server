package com.umc.bscene.domain.chat.dto.response;

import com.umc.bscene.domain.chat.enums.ChatContextType;
import java.util.List;

public record ChatRoomDetailResponse(
        Long chatRoomId,
        ChatContextType contextType,
        Long sessionApplicationId,
        Long sessionRecruitmentId,
        Long applicationSubmissionId,
        Long opponentUserId,
        String opponentName,
        String opponentProfileImageUrl,
        boolean canSend,
        List<ChatMessageDetailResponse> messages,
        int size,
        Long nextCursor,
        boolean hasNext
) {}
