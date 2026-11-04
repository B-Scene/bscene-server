package com.umc.bscene.domain.oauth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GoogleMobileLoginRequest(
        @NotBlank(message = "Google ID Token은 필수입니다.")
        @Size(max = 16384, message = "Google ID Token 길이가 올바르지 않습니다.")
        String idToken
) {
    @Override
    public String toString() {
        return "GoogleMobileLoginRequest[idToken=REDACTED]";
    }
}
