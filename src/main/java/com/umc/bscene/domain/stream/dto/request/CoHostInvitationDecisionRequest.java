package com.umc.bscene.domain.stream.dto.request;

import jakarta.validation.constraints.NotNull;

public record CoHostInvitationDecisionRequest(
        @NotNull(message = "공동 진행자 초대 수락·거절 값은 비어있을 수 없습니다.")
        Boolean isAccepted
) {
}