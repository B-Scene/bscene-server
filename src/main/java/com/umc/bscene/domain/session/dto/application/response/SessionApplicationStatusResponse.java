package com.umc.bscene.domain.session.dto.application.response;

import lombok.Builder;

@Builder
public record SessionApplicationStatusResponse(
        Long bandId
) {
    public static SessionApplicationStatusResponse of(Long bandId) {
        return SessionApplicationStatusResponse.builder()
                .bandId(bandId)
                .build();
    }
}