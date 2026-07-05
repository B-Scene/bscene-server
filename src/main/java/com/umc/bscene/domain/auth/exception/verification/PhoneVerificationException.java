package com.umc.bscene.domain.auth.exception.verification;

import com.umc.bscene.global.exception.BaseException;
import com.umc.bscene.global.response.code.BaseResponseCode;

public class PhoneVerificationException extends BaseException {

    public PhoneVerificationException(BaseResponseCode code) {
        super(code);
    }
}