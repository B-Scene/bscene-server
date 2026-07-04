package com.umc.bscene.domain.auth.dto.response;

public record SignupResponse(
        Long userId,
        Boolean onboardingCompleted
) {
}