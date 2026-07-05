package com.umc.bscene.domain.auth.dto.auth.response;

public record SignupResponse(
        Long userId,
        Boolean onboardingCompleted
) {
}