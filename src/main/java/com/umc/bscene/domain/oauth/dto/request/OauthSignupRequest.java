package com.umc.bscene.domain.oauth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OauthSignupRequest(

        @NotBlank
        String signupToken,

        @NotBlank
        @Size(max = 10)
        String name,

        @NotBlank
        @Pattern(regexp = "\\d{6}", message = "생년월일은 YYMMDD 형식이어야 합니다.")
        String birthDatePrefix,

        @NotBlank
        @Pattern(regexp = "[1-4]", message = "성별 코드는 1~4만 입력할 수 있습니다.")
        String genderCode,

        @NotBlank
        @Pattern(regexp = "^010\\d{8}$", message = "휴대폰 번호 형식이 올바르지 않습니다.")
        String phone
) {
}
