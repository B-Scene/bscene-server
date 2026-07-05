package com.umc.bscene.domain.auth.phoneverification.response.code;

import com.umc.bscene.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.umc.bscene.global.constant.StaticValue.BAD_GATEWAY;
import static com.umc.bscene.global.constant.StaticValue.BAD_REQUEST;

@Getter
@RequiredArgsConstructor
public enum PhoneVerificationErrorCode implements BaseResponseCode {

    INVALID_PHONE_VERIFICATION_REQUEST(BAD_REQUEST, "PHONE400_1", "휴대폰 인증 요청 값이 올바르지 않습니다."),
    VERIFICATION_CODE_MISMATCH(BAD_REQUEST, "PHONE400_2", "인증번호가 올바르지 않습니다."),
    VERIFICATION_CODE_NOT_FOUND(BAD_REQUEST, "PHONE400_3", "인증번호가 만료되었거나 존재하지 않습니다."),
    PHONE_VERIFICATION_REQUIRED(BAD_REQUEST, "PHONE400_4", "휴대폰 인증이 필요합니다."),
    PHONE_VERIFICATION_SEND_TOO_FREQUENT(BAD_REQUEST, "PHONE400_5", "인증번호는 일정 시간 후 다시 발송할 수 있습니다."),
    PHONE_VERIFICATION_SEND_LIMIT_EXCEEDED(BAD_REQUEST, "PHONE400_6", "일일 인증번호 발송 횟수를 초과했습니다."),
    PHONE_VERIFICATION_VERIFY_LIMIT_EXCEEDED(BAD_REQUEST, "PHONE400_7", "인증번호 인증 시도 횟수를 초과했습니다."),
    PHONE_VERIFICATION_DAILY_VERIFY_LIMIT_EXCEEDED(BAD_REQUEST, "PHONE400_8", "일일 인증번호 인증 횟수를 초과했습니다."),

    SMS_SEND_FAILED(BAD_GATEWAY, "PHONE502_1", "인증번호 문자 발송에 실패했습니다.");

    private final int status;
    private final String code;
    private final String message;
}