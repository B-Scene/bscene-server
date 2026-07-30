package com.umc.bscene.domain.stream.dto.request;

import jakarta.validation.constraints.NotNull;

public record CoHostUpgradeAcceptRequest(
        // 업그레이드를 요청한 밴드 멤버의 유저 ID (coHostUpgradeRequested SSE payload의 userId)
        @NotNull(message = "수락할 요청자의 userId는 비어있을 수 없습니다.")
        Long userId
) {
}
