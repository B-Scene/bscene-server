package com.umc.bscene.domain.oauth.dto.request;

import com.umc.bscene.domain.user.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OauthSignupRequest(

        @NotBlank
        String signupToken,

        @NotBlank
        @Size(max = 10)
        String name,

        @NotNull
        Gender gender,

        @NotBlank
        @Pattern(regexp = "^010\\d{8}$", message = "휴대폰 번호 형식이 올바르지 않습니다.")
        String phone
) {
}
