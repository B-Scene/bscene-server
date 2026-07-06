package com.umc.bscene.domain.session.exception;

import com.umc.bscene.global.exception.BaseException;
import com.umc.bscene.global.response.code.BaseResponseCode;

public class SessionProfileException extends BaseException {

    public SessionProfileException(BaseResponseCode code) {
        super(code);
    }
}