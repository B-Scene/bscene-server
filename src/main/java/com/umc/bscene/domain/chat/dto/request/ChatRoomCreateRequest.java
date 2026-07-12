package com.umc.bscene.domain.chat.dto.request;

import com.umc.bscene.domain.chat.enums.ChatContextType;
import jakarta.validation.constraints.NotNull;

public record ChatRoomCreateRequest(
        @NotNull ChatContextType contextType,
        Long sessionRecruitmentId,
        Long sessionApplicationId
) {
}
