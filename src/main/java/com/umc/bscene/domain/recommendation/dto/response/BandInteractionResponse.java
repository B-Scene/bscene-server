package com.umc.bscene.domain.recommendation.dto.response;

import com.umc.bscene.domain.recommendation.entity.BandInteraction;

import java.time.LocalDateTime;

public record BandInteractionResponse(
        Long bandId,
        Integer clickCount,
        LocalDateTime lastInteractedAt
) {
    public static BandInteractionResponse from(BandInteraction bandInteraction) {
        return new BandInteractionResponse(
                bandInteraction.getBand().getId(),
                bandInteraction.getClickCount(),
                bandInteraction.getLastInteractedAt()
        );
    }
}
