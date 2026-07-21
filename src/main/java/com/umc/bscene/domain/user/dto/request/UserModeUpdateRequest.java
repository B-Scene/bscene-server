package com.umc.bscene.domain.user.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UserModeUpdateRequest(
        @NotNull(message = "모드 변경 요청에는 반드시 id가 필요합니다.")
        @Min(value = 1, message = "유효하지 않은 id입니다.")
        Long requestId
) {
}
