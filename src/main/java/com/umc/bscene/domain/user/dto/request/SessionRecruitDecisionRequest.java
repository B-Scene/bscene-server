package com.umc.bscene.domain.user.dto.request;

import jakarta.validation.constraints.NotNull;

public record SessionRecruitDecisionRequest(
        @NotNull(message = "지원에 대한 수락, 거절 상태값은 비어있을 수 없습니다.")
        Boolean isApproved
) {
}
