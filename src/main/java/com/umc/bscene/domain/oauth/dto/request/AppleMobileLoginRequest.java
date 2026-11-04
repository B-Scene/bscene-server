package com.umc.bscene.domain.oauth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AppleMobileLoginRequest(
        @NotBlank(message = "Apple ID Token은 필수입니다.")
        @Size(max = 16384, message = "Apple ID Token 길이가 올바르지 않습니다.")
        String identityToken,
        @NotBlank(message = "Apple 인가 코드는 필수입니다.")
        @Size(max = 4096, message = "Apple 인가 코드 길이가 올바르지 않습니다.")
        @Pattern(regexp = "[\\x21-\\x7e]+", message = "Apple 인가 코드 형식이 올바르지 않습니다.")
        String authorizationCode,
        @NotBlank(message = "Apple 로그인 요청 ID는 필수입니다.")
        @Pattern(regexp = "[A-Za-z0-9_-]{43}", message = "Apple 로그인 요청 ID 형식이 올바르지 않습니다.")
        String challengeId
) {
    @Override
    public String toString() {
        return "AppleMobileLoginRequest[REDACTED]";
    }
}
