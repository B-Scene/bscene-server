package com.umc.bscene.domain.auth.dto.request;

import com.umc.bscene.domain.user.enums.Gender;
import jakarta.validation.constraints.*;

public record SignupRequest(
        @NotBlank
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 100)
        String loginId,

        @NotBlank
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*]).{8,20}$",
                message = "비밀번호는 영문, 숫자, 특수문자를 포함한 8~20자여야 합니다."
        )
        String password,

        @NotBlank
        String passwordConfirm,

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