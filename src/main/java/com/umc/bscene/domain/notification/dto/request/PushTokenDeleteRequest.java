package com.umc.bscene.domain.notification.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PushTokenDeleteRequest(
        @NotBlank String token
) {
}
