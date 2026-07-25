package com.umc.bscene.domain.user.dto.response.session;

public record SessionApplicationStatusResult(
        Long bandId,
        Long applicantUserId,
        String applicationNickname,
        String recruitmentTitle
) {
}