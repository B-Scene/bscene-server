package com.umc.bscene.domain.auth.exception;

import com.umc.bscene.global.exception.BaseException;
import com.umc.bscene.global.response.code.BaseResponseCode;

public class AuthException extends BaseException {

    public AuthException(BaseResponseCode baseResponseCode) {
        super(baseResponseCode);
    }
}