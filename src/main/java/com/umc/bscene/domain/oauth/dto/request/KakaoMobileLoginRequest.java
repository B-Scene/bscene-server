package com.umc.bscene.domain.oauth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 카카오 access token 은 보통 수십 자이지만 상한을 두어 비정상 입력을 걸러낸다
public record KakaoMobileLoginRequest(
        @NotBlank(message = "카카오 access token은 필수입니다.")
        @Size(max = 4096, message = "카카오 access token 길이가 올바르지 않습니다.")
        String accessToken
) {
    @Override
    public String toString() {
        return "KakaoMobileLoginRequest[accessToken=REDACTED]";
    }
}
