package com.umc.bscene.domain.auth.phoneverification.exception;

import com.umc.bscene.global.exception.BaseException;
import com.umc.bscene.global.response.code.BaseResponseCode;

public class PhoneVerificationException extends BaseException {

    public PhoneVerificationException(BaseResponseCode code) {
        super(code);
    }
}