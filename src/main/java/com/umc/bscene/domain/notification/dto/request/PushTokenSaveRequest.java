package com.umc.bscene.domain.notification.dto.request;

import com.umc.bscene.domain.notification.enums.PushPlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PushTokenSaveRequest(
        @NotBlank String token,
        @NotNull PushPlatform platform
) {
}
