package com.umc.bscene.domain.user.dto.request;

import jakarta.validation.constraints.NotNull;

public record UserBlockRequest(
        @NotNull(message = "liveId 필드는 필수 값입니다.")
        Long liveId,
        @NotNull(message = "targetUserId 필드는 필수 값입니다.")
        Long targetUserId
) {
}
