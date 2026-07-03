package com.umc.bscene.domain.phoneverification.dto.request;

import com.umc.bscene.domain.phoneverification.enums.PhoneVerificationPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhoneVerificationVerifyRequest {

    @NotBlank(message = "휴대폰 번호는 필수입니다.")
    @Pattern(regexp = "^010\\d{8}$", message = "휴대폰 번호 형식이 올바르지 않습니다.")
    private String phone;

    @NotBlank(message = "인증번호는 필수입니다.")
    @Pattern(regexp = "^\\d{6}$", message = "인증번호는 6자리 숫자입니다.")
    private String code;

    @NotNull(message = "인증 목적은 필수입니다.")
    private PhoneVerificationPurpose purpose;
}
