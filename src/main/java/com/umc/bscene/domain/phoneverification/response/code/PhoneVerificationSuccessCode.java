package com.umc.bscene.domain.phoneverification.response.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.umc.bscene.global.constant.StaticValue.OK;

@Getter
@RequiredArgsConstructor
public enum PhoneVerificationSuccessCode implements BaseResponseCode {

    PHONE_VERIFICATION_SEND_SUCCESS(OK, "PHONE200_1", "인증번호가 발송되었습니다."),
    PHONE_VERIFICATION_VERIFY_SUCCESS(OK, "PHONE200_2", "휴대폰 인증이 완료되었습니다.");

    private final int status;
    private final String code;
    private final String message;
}
