package com.umc.bscene.domain.session.dto.application.response;

public record SessionApplicationVisibilityResponse(
        Long sessionApplicationId,
        boolean isPublic
) {
}
