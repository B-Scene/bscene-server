package com.umc.bscene.domain.auth.dto.response;

import com.umc.bscene.domain.user.enums.UserMode;

public record LoginUserResponse(
        Long userId,
        String name,
        UserMode currentMode,
        Boolean onboardingCompleted
) {
}