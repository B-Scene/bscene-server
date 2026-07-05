package com.umc.bscene.domain.auth.onboarding.dto.response;

import com.umc.bscene.domain.user.enums.UserMode;

import java.util.List;

public record OnboardingStatusResponse(
        Boolean completed,
        UserMode currentMode,
        List<UserMode> availableModes,
        String fanNickname,
        List<GenreResponse> selectedGenres,
        List<RegionResponse> selectedRegions,
        List<String> requiredSteps
) {
}