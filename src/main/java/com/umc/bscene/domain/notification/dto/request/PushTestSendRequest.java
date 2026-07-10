package com.umc.bscene.domain.notification.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PushTestSendRequest(
        @NotBlank String title,
        @NotBlank String body
) {
}