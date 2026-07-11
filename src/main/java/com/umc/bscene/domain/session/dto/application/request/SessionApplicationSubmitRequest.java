package com.umc.bscene.domain.session.dto.application.request;

import jakarta.validation.constraints.NotNull;

public record SessionApplicationSubmitRequest(
        @NotNull(message = "지원서 ID는 필수입니다.")
        Long sessionApplicationId
) {
}
