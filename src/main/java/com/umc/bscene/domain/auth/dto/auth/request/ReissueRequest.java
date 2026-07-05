package com.umc.bscene.domain.auth.dto.auth.request;

import jakarta.validation.constraints.NotBlank;

public record ReissueRequest(
        @NotBlank
        String refreshToken
) {
}